package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record ProgramMapConfig(AccountMeta readCpiProgram,
                               List<IxMapConfig> ixMapConfigs,
                               int discriminatorLength) {

  public PublicKey cpiProgram() {
    return readCpiProgram.publicKey();
  }

  public boolean fixedLengthDiscriminator() {
    return discriminatorLength > 0;
  }

  public <A> ProgramProxy<A> createProgramProxy(final AccountMeta invokedProxyProgram,
                                                final Function<DynamicAccountConfig, DynamicAccount<A>> dynamicAccountFactory) {
    if (fixedLengthDiscriminator()) {
      final var ixProxies = HashMap.<Discriminator, IxProxy<A>>newHashMap(ixMapConfigs.size());
      for (final var ixMapConfig : ixMapConfigs) {
        final var ixProxy = ixMapConfig.createProxy(readCpiProgram, invokedProxyProgram, dynamicAccountFactory);
        ixProxies.put(ixProxy.cpiDiscriminator(), ixProxy);
      }
      return ProgramProxy.createProxy(discriminatorLength, ixProxies);
    } else {
      final var ixProxies = new ArrayList<IxProxy<A>>(ixMapConfigs.size());
      for (final var ixMapConfig : ixMapConfigs) {
        final var ixProxy = ixMapConfig.createProxy(readCpiProgram, invokedProxyProgram, dynamicAccountFactory);
        ixProxies.add(ixProxy);
      }
      return ProgramProxy.createProxy(ixProxies);
    }
  }

  public static <A> Map<PublicKey, ProgramProxy<A>> createProgramProxies(final AccountMeta invokedProxyProgram,
                                                                         final Function<DynamicAccountConfig, DynamicAccount<A>> dynamicAccountFactory,
                                                                         final List<ProgramMapConfig> programMapConfigs) {
    final var proxies = new HashMap<PublicKey, ProgramProxy<A>>(programMapConfigs.size());
    for (final var programMapConfig : programMapConfigs) {
      proxies.put(programMapConfig.cpiProgram(), programMapConfig.createProgramProxy(invokedProxyProgram, dynamicAccountFactory));
    }
    return proxies;
  }

  static CharBufferFunction<PublicKey> PARSE_BASE58_PUBLIC_KEY = PublicKey::fromBase58Encoded;

  public static ProgramMapConfig parseConfig(final Map<AccountMeta, AccountMeta> accountMetaCache,
                                             final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache,
                                             final JsonIterator ji) {
    final var parser = new Parser(accountMetaCache, indexedAccountMetaCache);
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private final Map<AccountMeta, AccountMeta> accountMetaCache;
    private final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache;

    private PublicKey program;
    private List<IxMapConfig> ixMapConfigs;

    private Parser(final Map<AccountMeta, AccountMeta> accountMetaCache,
                   final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache) {
      this.accountMetaCache = accountMetaCache;
      this.indexedAccountMetaCache = indexedAccountMetaCache;
    }

    private ProgramMapConfig create() {
      final var readProgram = AccountMeta.createRead(program);
      if (ixMapConfigs.isEmpty()) {
        return new ProgramMapConfig(readProgram, ixMapConfigs, 0);
      } else {
        final var iterator = ixMapConfigs.iterator();
        var ixMapConfig = iterator.next();
        int discriminatorLength = ixMapConfig.proxyDiscriminator().length();
        while (iterator.hasNext()) {
          ixMapConfig = iterator.next();
          final int len = ixMapConfig.proxyDiscriminator().length();
          if (len != discriminatorLength) {
            return new ProgramMapConfig(readProgram, ixMapConfigs, -1);
          }
        }
        return new ProgramMapConfig(readProgram, ixMapConfigs, discriminatorLength);
      }
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("program_id", buf, offset, len)) {
        this.program = ji.applyChars(PARSE_BASE58_PUBLIC_KEY);
      } else if (fieldEquals("instructions", buf, offset, len)) {
        final var ixMapConfigs = new ArrayList<IxMapConfig>();
        while (ji.readArray()) {
          ixMapConfigs.add(IxMapConfig.parseConfig(accountMetaCache, indexedAccountMetaCache, ji));
        }
        this.ixMapConfigs = ixMapConfigs;
      } else {
        throw new IllegalStateException("Unknown ProgramMapConfig field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

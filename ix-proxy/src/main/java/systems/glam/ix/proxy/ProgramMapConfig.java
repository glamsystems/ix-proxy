package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record ProgramMapConfig(Collection<AccountMeta> programs,
                               List<IxMapConfig> ixMapConfigs,
                               int discriminatorLength) {

  public boolean fixedLengthDiscriminator() {
    return discriminatorLength > 0;
  }

  public <A> Collection<ProgramProxy<A>> createProgramProxies(final AccountMeta invokedProxyProgram,
                                                              final Function<DynamicAccountConfig, DynamicAccount<A>> dynamicAccountFactory) {
    if (fixedLengthDiscriminator()) {
      final var ixProxies = HashMap.<Discriminator, IxProxy<A>>newHashMap(ixMapConfigs.size());
      for (final var ixMapConfig : ixMapConfigs) {
        final var ixProxy = ixMapConfig.createProxy(invokedProxyProgram, dynamicAccountFactory);
        ixProxies.put(ixProxy.cpiDiscriminator(), ixProxy);
      }
      return programs.stream()
          .map(program -> ProgramProxy.createProxy(program, discriminatorLength, ixProxies))
          .toList();
    } else {
      final var ixProxies = new ArrayList<IxProxy<A>>(ixMapConfigs.size());
      for (final var ixMapConfig : ixMapConfigs) {
        final var ixProxy = ixMapConfig.createProxy(invokedProxyProgram, dynamicAccountFactory);
        ixProxies.add(ixProxy);
      }
      return programs.stream()
          .map(program -> ProgramProxy.createProxy(program, ixProxies))
          .toList();
    }
  }

  public static <A> Map<PublicKey, ProgramProxy<A>> createProgramProxies(final AccountMeta invokedProxyProgram,
                                                                         final Function<DynamicAccountConfig, DynamicAccount<A>> dynamicAccountFactory,
                                                                         final List<ProgramMapConfig> programMapConfigs) {
    final var proxies = new HashMap<PublicKey, ProgramProxy<A>>(programMapConfigs.size());
    for (final var programMapConfig : programMapConfigs) {
      final var programProxies = programMapConfig.createProgramProxies(invokedProxyProgram, dynamicAccountFactory);
      for (final var programProxy : programProxies) {
        proxies.put(programProxy.cpiProgram(), programProxy);
      }
    }
    return proxies;
  }

  public static <A> void createProxies(final Path mappingFile,
                                       final AccountMeta invokedProxyProgram,
                                       final Map<PublicKey, ProgramProxy<A>> programProxiesOutput,
                                       final Function<DynamicAccountConfig, DynamicAccount<A>> dynamicAccountFactory,
                                       final Map<AccountMeta, AccountMeta> accountMetaCache,
                                       final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache) {
    try {
      final var mappingJson = Files.readAllBytes(mappingFile);
      final var ji = JsonIterator.parse(mappingJson);
      final var programMapConfig = ProgramMapConfig.parseConfig(accountMetaCache, indexedAccountMetaCache, ji);
      final var programProxyCollection = programMapConfig.createProgramProxies(invokedProxyProgram, dynamicAccountFactory);
      for (final var proxy : programProxyCollection) {
        programProxiesOutput.put(proxy.cpiProgram(), proxy);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
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

    private Collection<AccountMeta> programs;
    private List<IxMapConfig> ixMapConfigs;

    private Parser(final Map<AccountMeta, AccountMeta> accountMetaCache,
                   final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache) {
      this.accountMetaCache = accountMetaCache;
      this.indexedAccountMetaCache = indexedAccountMetaCache;
    }

    private ProgramMapConfig create() {
      if (ixMapConfigs.isEmpty()) {
        return new ProgramMapConfig(programs, ixMapConfigs, 0);
      } else {
        final var iterator = ixMapConfigs.iterator();
        final int discriminatorLength = iterator.next().cpiDiscriminator().length();
        while (iterator.hasNext()) {
          if (iterator.next().cpiDiscriminator().length() != discriminatorLength) {
            return new ProgramMapConfig(programs, ixMapConfigs, -1);
          }
        }
        return new ProgramMapConfig(programs, ixMapConfigs, discriminatorLength);
      }
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("program_id", buf, offset, len)) {
        if (ji.whatIsNext() == ValueType.ARRAY) {
          final var programs = new HashSet<AccountMeta>();
          while (ji.readArray()) {
            programs.add(AccountMeta.createRead(ji.applyChars(PARSE_BASE58_PUBLIC_KEY)));
          }
          this.programs = List.copyOf(programs);
        } else {
          this.programs = List.of(AccountMeta.createRead(ji.applyChars(PARSE_BASE58_PUBLIC_KEY)));
        }
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

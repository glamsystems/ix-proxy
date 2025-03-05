package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record IxMapConfig(String cpiIxName,
                          Discriminator cpiDiscriminator,
                          String proxyIxName,
                          Discriminator proxyDiscriminator,
                          List<DynamicAccountConfig> dynamicAccounts,
                          List<IndexedAccountMeta> staticAccounts,
                          int[] indexMap) {

  public static IxMapConfig parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  public <A> IxProxy<A> createProxy(final AccountMeta readCpiProgram,
                                    final AccountMeta invokedProxyProgram,
                                    final Function<DynamicAccountConfig, DynamicAccount<A>> accountMetaFactory) {
    return IxProxy.createProxy(
        readCpiProgram,
        invokedProxyProgram,
        cpiDiscriminator,
        proxyDiscriminator,
        dynamicAccounts.stream().map(accountMetaFactory).toList(),
        staticAccounts,
        indexMap
    );
  }

  private static final class Parser implements FieldBufferPredicate {

    private static final List<IndexedAccountMeta> NO_NEW_ACCOUNTS = List.of();
    private static final int[] NO_INDEX_MAP = new int[0];

    private String cpiIxName;
    private Discriminator cpiDiscriminator;
    private String proxyIxName;
    private Discriminator proxyDiscriminator;
    private List<DynamicAccountConfig> dynamicAccounts;
    private List<IndexedAccountMeta> staticAccounts;
    private int[] indexMap;

    private Parser() {
    }

    private IxMapConfig create() {
      return new IxMapConfig(
          cpiIxName,
          cpiDiscriminator,
          proxyIxName,
          proxyDiscriminator,
          dynamicAccounts,
          staticAccounts == null ? NO_NEW_ACCOUNTS : staticAccounts,
          indexMap == null ? NO_INDEX_MAP : indexMap
      );
    }

    private static Discriminator parseDiscriminator(final JsonIterator ji) {
      int i = 0;
      final int mark = ji.mark();
      while (ji.readArray()) {
        ji.skip();
        ++i;
      }
      final byte[] discriminator = new byte[i];
      ji.reset(mark);
      for (i = 0; ji.readArray(); ++i) {
        discriminator[i] = (byte) ji.readInt();
      }
      return Discriminator.createDiscriminator(discriminator);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("src_ix_name", buf, offset, len)) {
        cpiIxName = ji.readString();
      } else if (fieldEquals("src_discriminator", buf, offset, len)) {
        cpiDiscriminator = parseDiscriminator(ji);
      } else if (fieldEquals("dst_ix_name", buf, offset, len)) {
        proxyIxName = ji.readString();
      } else if (fieldEquals("dst_discriminator", buf, offset, len)) {
        proxyDiscriminator = parseDiscriminator(ji);
      } else if (fieldEquals("dynamic_accounts", buf, offset, len)) {
        final var programAccounts = new ArrayList<DynamicAccountConfig>();
        while (ji.readArray()) {
          programAccounts.add(DynamicAccountConfig.parseConfig(ji));
        }
        this.dynamicAccounts = programAccounts;
      } else if (fieldEquals("static_accounts", buf, offset, len)) {
        final var newAccounts = new ArrayList<IndexedAccountMeta>();
        while (ji.readArray()) {
          newAccounts.add(IndexedAccountMeta.parseConfig(ji));
        }
        this.staticAccounts = newAccounts.isEmpty() ? NO_NEW_ACCOUNTS : newAccounts;
      } else if (fieldEquals("index_map", buf, offset, len)) {
        int i = 0;
        final int mark = ji.mark();
        for (; ji.readArray(); ++i) {
          ji.skip();
        }
        if (i > 0) {
          ji.reset(mark);
          final int[] indexMap = new int[i];
          for (i = 0; ji.readArray(); ++i) {
            indexMap[i] = ji.readInt();
          }
          this.indexMap = indexMap;
        } else {
          this.indexMap = NO_INDEX_MAP;
        }
      } else {
        throw new IllegalStateException("Unknown IxMapConfig field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

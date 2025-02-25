package systems.glam.ix.converter;

import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record IxMapConfig(String srcIxName,
                          Discriminator srcDiscriminator,
                          String dstIxName,
                          Discriminator dstDiscriminator,
                          List<ProgramAccountConfig> programAccounts,
                          List<IndexedAccountMeta> newAccounts,
                          int[] indexMap) {

  public static IxMapConfig parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  public IxProxy createProxy(final Function<ProgramAccountConfig, IndexedAccountMeta> accountMetaFactory) {
    return IxProxy.createProxy(
        srcDiscriminator,
        dstDiscriminator,
        programAccounts.stream().map(accountMetaFactory).toList(),
        newAccounts,
        indexMap
    );
  }

  private static final class Parser implements FieldBufferPredicate {

    private static final List<IndexedAccountMeta> NO_NEW_ACCOUNTS = List.of();
    private static final int[] NO_INDEX_MAP = new int[0];

    private String srcIxName;
    private Discriminator srcDiscriminator;
    private String dstIxName;
    private Discriminator dstDiscriminator;
    private List<ProgramAccountConfig> programAccounts;
    private List<IndexedAccountMeta> newAccounts;
    private int[] indexMap;

    private Parser() {
    }

    private IxMapConfig create() {
      return new IxMapConfig(
          srcIxName,
          srcDiscriminator,
          dstIxName,
          dstDiscriminator,
          programAccounts,
          newAccounts == null ? NO_NEW_ACCOUNTS : newAccounts,
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
      if (fieldEquals("ix_name", buf, offset, len)) {
        srcIxName = ji.readString();
      } else if (fieldEquals("ix_discriminator", buf, offset, len)) {
        srcDiscriminator = parseDiscriminator(ji);
      } else if (fieldEquals("glam_ix_name", buf, offset, len)) {
        dstIxName = ji.readString();
      } else if (fieldEquals("glam_ix_discriminator", buf, offset, len)) {
        dstDiscriminator = parseDiscriminator(ji);
      } else if (fieldEquals("glam_accounts", buf, offset, len)) {
        final var programAccounts = new ArrayList<ProgramAccountConfig>();
        while (ji.readArray()) {
          programAccounts.add(ProgramAccountConfig.parseConfig(ji));
        }
        this.programAccounts = programAccounts;
      } else if (fieldEquals("new_accounts", buf, offset, len)) {
        final var newAccounts = new ArrayList<IndexedAccountMeta>();
        while (ji.readArray()) {
          newAccounts.add(IndexedAccountMeta.parseConfig(ji));
        }
        this.newAccounts = newAccounts;
      } else if (fieldEquals("index_map", buf, offset, len)) {
        final var indexMap = new ArrayList<Integer>();
        while (ji.readArray()) {
          ji.skipObjField();
          indexMap.add(ji.readInt());
          ji.closeObj();
        }
        this.indexMap = indexMap.isEmpty() ? NO_INDEX_MAP : indexMap.stream().mapToInt(i -> i).toArray();
      } else {
        throw new IllegalStateException("Unknown IxMapConfig field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

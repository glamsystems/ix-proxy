package systems.glam.ix.converter;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record ProgramAccountConfig(String name,
                                   int index,
                                   boolean writable,
                                   boolean signer) {

  public static ProgramAccountConfig parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private String name;
    private int index;
    private boolean writable;
    private boolean signer;

    private Parser() {
    }

    private ProgramAccountConfig create() {
      return new ProgramAccountConfig(name, index, writable, signer);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("name", buf, offset, len)) {
        name = ji.readString();
      } else if (fieldEquals("index", buf, offset, len)) {
        index = ji.readInt();
      } else if (fieldEquals("writable", buf, offset, len)) {
        writable = ji.readBoolean();
      } else if (fieldEquals("signer", buf, offset, len)) {
        signer = ji.readBoolean();
      } else {
        throw new IllegalStateException("Unknown ProgramAccountConfig field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

package systems.glam.ix.converter;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record ProgramMapConfig(PublicKey program, List<IxMapConfig> ixMapConfigs) {

  public static ProgramMapConfig parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey program;
    private List<IxMapConfig> ixMapConfigs;

    private Parser() {
    }

    private ProgramMapConfig create() {
      return new ProgramMapConfig(program, ixMapConfigs);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("program_id", buf, offset, len)) {
        program = PublicKey.fromBase58Encoded(buf, offset, len);
      } else if (fieldEquals("instructions", buf, offset, len)) {
        final var ixMapConfigs = new ArrayList<IxMapConfig>();
        while (ji.readArray()) {
          ixMapConfigs.add(IxMapConfig.parseConfig(ji));
        }
        this.ixMapConfigs = ixMapConfigs;
      } else {
        throw new IllegalStateException("Unknown ProgramMapConfig field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

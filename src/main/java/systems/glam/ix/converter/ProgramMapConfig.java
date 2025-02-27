package systems.glam.ix.converter;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record ProgramMapConfig(PublicKey program,
                               List<IxMapConfig> ixMapConfigs,
                               int discriminatorLength) {

  public boolean fixedLengthDiscriminator() {
    return discriminatorLength() > 0;
  }

  static CharBufferFunction<PublicKey> PARSE_BASE58_PUBLIC_KEY = PublicKey::fromBase58Encoded;

  public static ProgramMapConfig parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey program;
    private List<IxMapConfig> ixMapConfigs;
    private int discriminatorLength = -1;

    private Parser() {
    }

    private ProgramMapConfig create() {
      return new ProgramMapConfig(program, ixMapConfigs, discriminatorLength);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("program_id", buf, offset, len)) {
        program = ji.applyChars(PARSE_BASE58_PUBLIC_KEY);
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

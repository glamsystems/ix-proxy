package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record ProgramMapConfig(AccountMeta readProgram,
                               List<IxMapConfig> ixMapConfigs,
                               int discriminatorLength) {

  public PublicKey program() {
    return readProgram.publicKey();
  }

  public boolean fixedLengthDiscriminator() {
    return discriminatorLength > 0;
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

    private Parser() {
    }

    private ProgramMapConfig create() {
      final var readProgram = AccountMeta.createRead(program);
      if (ixMapConfigs.isEmpty()) {
        return new ProgramMapConfig(readProgram, ixMapConfigs, 0);
      } else {
        final var iterator = ixMapConfigs.iterator();
        var ixMapConfig = iterator.next();
        int discriminatorLength = ixMapConfig.dstDiscriminator().length();
        while (iterator.hasNext()) {
          ixMapConfig = iterator.next();
          final int len = ixMapConfig.dstDiscriminator().length();
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

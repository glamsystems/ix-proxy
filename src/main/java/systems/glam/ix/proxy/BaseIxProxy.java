package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;
import java.util.Base64;

abstract class BaseIxProxy<A> implements IxProxy<A> {

  final AccountMeta readCpiProgram;
  final AccountMeta invokedProxyProgram;
  protected final Discriminator cpiDiscriminator;
  private final byte[] cpiDiscriminatorBytes;

  protected BaseIxProxy(final AccountMeta readCpiProgram,
                        final AccountMeta invokedProxyProgram,
                        final Discriminator cpiDiscriminator) {
    this.readCpiProgram = readCpiProgram;
    this.invokedProxyProgram = invokedProxyProgram;
    this.cpiDiscriminator = cpiDiscriminator;
    this.cpiDiscriminatorBytes = cpiDiscriminator.data();
  }

  protected final void validateMapping(final Instruction instruction) {
    if (!instruction.programId().publicKey().equals(readCpiProgram.publicKey())) {
      throw new IllegalStateException(String.format("""
              Expected CPI program to be %s, but was %s for invoked proxy program %s.""",
          readCpiProgram.publicKey(), instruction.programId().publicKey(), invokedProxyProgram.publicKey()
      ));
    }

    final int cpiDiscriminatorLength = cpiDiscriminator.length();
    if (cpiDiscriminatorBytes.length != cpiDiscriminatorLength) {
      throw new IllegalStateException(String.format(
          "Expected CPI discriminator length of %d, but was %d.",
          cpiDiscriminatorBytes.length, cpiDiscriminatorLength
      ));
    }

    final int cpiDataOffset = instruction.offset();
    final byte[] cpiData = instruction.data();
    if (!Arrays.equals(
        cpiData, cpiDataOffset, cpiDataOffset + cpiDiscriminatorLength,
        cpiDiscriminatorBytes, 0, cpiDiscriminatorLength
    )) {
      throw new IllegalStateException(String.format(
          "Expected CPI discriminator %s, but was %s.",
          Base64.getEncoder().encodeToString(cpiDiscriminatorBytes),
          Base64.getEncoder().encodeToString(Arrays.copyOfRange(cpiData, cpiDataOffset, cpiDataOffset + cpiDiscriminatorLength))
      ));
    }
  }

  @Override
  public final Instruction mapInstruction(final AccountMeta feePayer,
                                          final A runtimeAccounts,
                                          final Instruction instruction) {
    validateMapping(instruction);
    return mapInstructionUnchecked(feePayer, runtimeAccounts, instruction);
  }

  @Override
  public final boolean matchesCpiDiscriminator(final byte[] instructionData, final int offset, final int length) {
    final int discriminatorLength = cpiDiscriminatorBytes.length;
    if (discriminatorLength <= length) {
      return Arrays.equals(
          instructionData, offset, offset + discriminatorLength,
          cpiDiscriminatorBytes, 0, discriminatorLength
      );
    } else {
      return false;
    }
  }

  @Override
  public final Discriminator cpiDiscriminator() {
    return cpiDiscriminator;
  }
}

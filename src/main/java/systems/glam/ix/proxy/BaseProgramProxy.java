package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.tx.Instruction;

import java.util.Base64;

abstract class BaseProgramProxy<A> implements ProgramProxy<A> {

  private final AccountMeta readCpiProgram;

  protected BaseProgramProxy(final AccountMeta readCpiProgram) {
    this.readCpiProgram = readCpiProgram;
  }

  protected abstract IxProxy<A> lookupProxy(final Instruction instruction);

  private IxProxy<A> lookupProxyOrThrow(final Instruction instruction) {
    final var proxy = lookupProxy(instruction);
    if (proxy == null) {
      throw new IllegalStateException("Unsupported instruction: " + Base64.getEncoder().encodeToString(instruction.data()));
    } else {
      return proxy;
    }
  }

  @Override
  public final PublicKey cpiProgram() {
    return readCpiProgram.publicKey();
  }

  @Override
  public final Instruction mapInstruction(final AccountMeta feePayer,
                                          final A runtimeAccounts,
                                          final Instruction instruction) {
    final var proxy = lookupProxyOrThrow(instruction);
    return proxy.mapInstruction(readCpiProgram, feePayer, runtimeAccounts, instruction);
  }

  @Override
  public final Instruction mapInstructionUnchecked(final AccountMeta feePayer,
                                                   final A runtimeAccounts,
                                                   final Instruction instruction) {
    final var proxy = lookupProxyOrThrow(instruction);
    return proxy.mapInstructionUnchecked(readCpiProgram, feePayer, runtimeAccounts, instruction);
  }
}

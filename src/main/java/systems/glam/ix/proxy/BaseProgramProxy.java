package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.tx.Instruction;

import java.util.Base64;

abstract class BaseProgramProxy<A> implements ProgramProxy<A> {

  protected BaseProgramProxy() {
  }

  protected abstract IxProxy<A> lookupProxy(final Instruction instruction);

  @Override
  public final Instruction apply(final AccountMeta feePayer, final A runtimeAccounts, final Instruction instruction) {
    final var proxy = lookupProxy(instruction);
    if (proxy == null) {
      throw new IllegalStateException("Unsupported instruction: " + Base64.getEncoder().encodeToString(instruction.data()));
    } else {
      return proxy.mapInstruction(feePayer, runtimeAccounts, instruction);
    }
  }
}

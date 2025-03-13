
package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.tx.Instruction;

public interface IxMapper<A> {
  Instruction mapInstruction(final AccountMeta feePayer,
                             final A runtimeAccounts,
                             final Instruction instruction);

  /// Does not validate the expected program id or discriminators from the given instruction.
  Instruction mapInstructionUnchecked(final AccountMeta feePayer,
                                      final A runtimeAccounts,
                                      final Instruction instruction);
}

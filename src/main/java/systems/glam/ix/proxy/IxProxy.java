package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;

public interface IxProxy<A> {

  static <A> IxProxy<A> createProxy(final AccountMeta readCpiProgram,
                                    final AccountMeta invokedProxyProgram,
                                    final Discriminator cpiDiscriminator,
                                    final Discriminator proxyDiscriminator,
                                    final List<DynamicAccount<A>> dynamicAccounts,
                                    final List<IndexedAccountMeta> staticAccounts,
                                    final int[] indexes) {
    int numRemoved = 0;
    for (final int index : indexes) {
      if (index < 0) {
        ++numRemoved;
      }
    }
    return new IxProxyRecord<>(
        readCpiProgram,
        invokedProxyProgram,
        cpiDiscriminator,
        proxyDiscriminator,
        dynamicAccounts,
        staticAccounts,
        indexes,
        dynamicAccounts.size() + staticAccounts.size() + (indexes.length - numRemoved)
    );
  }

  Instruction mapInstruction(final AccountMeta feePayer,
                             final A runtimeAccounts,
                             final Instruction instruction);

  /// Does not validate the expected program id or discriminators from the given instruction.
  Instruction mapInstructionUnchecked(final AccountMeta feePayer,
                                      final A runtimeAccounts,
                                      final Instruction instruction);

  Discriminator cpiDiscriminator();

  boolean matchesCpiDiscriminator(final byte[] instructionData,
                                  final int offset,
                                  final int length);

  default boolean matchesCpiDiscriminator(final Instruction instruction) {
    return matchesCpiDiscriminator(instruction.data(), instruction.offset(), instruction.len());
  }

  Discriminator proxyDiscriminator();
}

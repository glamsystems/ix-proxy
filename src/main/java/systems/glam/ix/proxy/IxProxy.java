package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;
import java.util.stream.IntStream;

public interface IxProxy<A> {

  static <A> IxProxy<A> createProxy(final Discriminator srcDiscriminator,
                                    final Discriminator dstDiscriminator,
                                    final List<DynamicAccount<A>> dynamicAccounts,
                                    final List<IndexedAccountMeta> staticAccounts,
                                    final int[] indexes) {
    final int numRemoved = (int) IntStream.of(indexes).filter(i -> i < 0).count();
    return new IxProxyRecord<>(
        srcDiscriminator,
        dstDiscriminator,
        dynamicAccounts,
        staticAccounts,
        indexes,
        dynamicAccounts.size() + staticAccounts.size() + (indexes.length - numRemoved)
    );
  }

  Instruction mapInstruction(final AccountMeta invokedProgram,
                             final AccountMeta feePayer,
                             final A runtimeAccounts,
                             final Instruction instruction);

  default Instruction mapInstruction(final AccountMeta feePayer,
                                     final A runtimeAccounts,
                                     final Instruction instruction) {
    return mapInstruction(instruction.programId(), feePayer, runtimeAccounts, instruction);
  }

  Discriminator srcDiscriminator();

  Discriminator dstDiscriminator();
}

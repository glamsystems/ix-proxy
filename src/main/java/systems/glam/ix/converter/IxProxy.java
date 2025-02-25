package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;
import java.util.stream.IntStream;

public interface IxProxy<A extends ProgramAccounts<A>> {

  static <A extends ProgramAccounts<A>> IxProxy<A> createProxy(final Discriminator discriminator,
                                                               final Discriminator glamDiscriminator,
                                                               final List<DynamicAccountMeta<A>> newDynamicAccounts,
                                                               final List<IndexedAccountMeta> newAccounts,
                                                               final int[] indexes) {
    final int numRemoved = (int) IntStream.of(indexes).filter(i -> i < 0).count();
    return new IxProxyRecord<>(
        discriminator,
        glamDiscriminator,
        newDynamicAccounts,
        newAccounts,
        indexes,
        newDynamicAccounts.size() + newAccounts.size() + (indexes.length - numRemoved)
    );
  }

  Instruction mapInstruction(final AccountMeta invokedProgram, final A programAccounts, final Instruction instruction);
}

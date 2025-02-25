package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;
import java.util.stream.IntStream;

public interface IxProxy<A> {

  static <A> IxProxy<A> createProxy(final Discriminator discriminator,
                                    final Discriminator glamDiscriminator,
                                    final List<DynamicAccount<A>> dynamicAccounts,
                                    final List<IndexedAccountMeta> staticAccounts,
                                    final int[] indexes) {
    final int numRemoved = (int) IntStream.of(indexes).filter(i -> i < 0).count();
    return new IxProxyRecord<>(
        discriminator,
        glamDiscriminator,
        dynamicAccounts,
        staticAccounts,
        indexes,
        dynamicAccounts.size() + staticAccounts.size() + (indexes.length - numRemoved)
    );
  }

  Instruction mapInstruction(final AccountMeta invokedProgram, final A runtimeAccounts, final Instruction instruction);
}

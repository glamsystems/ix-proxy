package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;
import java.util.stream.IntStream;

public interface IxProxy {

  static IxProxy createProxy(final Discriminator discriminator,
                             final Discriminator glamDiscriminator,
                             final List<IndexedAccountMeta> programAccounts,
                             final List<IndexedAccountMeta> newAccounts,
                             final int[] indexes) {
    final int numRemoved = (int) IntStream.of(indexes).filter(i -> i < 0).count();
    return new IxProxyRecord(
        discriminator,
        glamDiscriminator,
        programAccounts,
        newAccounts,
        indexes,
        programAccounts.size() + newAccounts.size() + (indexes.length - numRemoved)
    );
  }

  Instruction mapInstruction(final AccountMeta invokedProgram, final Instruction instruction);
}

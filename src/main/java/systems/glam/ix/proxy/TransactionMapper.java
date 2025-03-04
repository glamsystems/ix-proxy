package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.lookup.AddressLookupTable;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.accounts.meta.LookupTableAccountMeta;
import software.sava.core.tx.Instruction;
import software.sava.core.tx.Transaction;

import java.util.List;
import java.util.Map;

public interface TransactionMapper<A> extends ProgramProxy<A> {

  static <A> TransactionMapper<A> createProxy(final Map<PublicKey, ProgramProxy<A>> programProxyMap) {
    return new ProgramProxyMap<>(programProxyMap);
  }

  Instruction[] mapInstructions(final AccountMeta feePayer,
                                final A runtimeAccounts,
                                final List<Instruction> instructions);

  Transaction mapTransaction(final AccountMeta feePayer,
                             final A runtimeAccounts,
                             final Transaction transaction);

  Transaction mapTransaction(final A runtimeAccounts, final Transaction transaction);

  Transaction mapTransactionWithTable(final AccountMeta feePayer,
                                      final A runtimeAccounts,
                                      final Transaction transaction,
                                      final AddressLookupTable addTable);

  Transaction mapTransactionWithTable(final A runtimeAccounts,
                                      final Transaction transaction,
                                      final AddressLookupTable addTable);

  Transaction mapTransactionWithTables(final AccountMeta feePayer,
                                       final A runtimeAccounts,
                                       final Transaction transaction,
                                       final LookupTableAccountMeta[] addTables);

  Transaction mapTransactionWithTables(final A runtimeAccounts,
                                       final Transaction transaction,
                                       final LookupTableAccountMeta[] addTables);
}

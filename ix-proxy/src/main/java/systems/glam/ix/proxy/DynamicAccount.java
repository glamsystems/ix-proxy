package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;

@FunctionalInterface
public interface DynamicAccount<A> {

  void setAccount(final AccountMeta[] mappedAccounts,
                  final AccountMeta cpiProgram,
                  final AccountMeta feePayer,
                  final A runtimeAccounts);
}

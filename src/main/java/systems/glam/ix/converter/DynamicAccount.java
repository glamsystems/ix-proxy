package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;

public interface DynamicAccount<A> {

  void setAccount(final AccountMeta[] mappedAccounts, final AccountMeta feePayer, final A runtimeAccounts);
}

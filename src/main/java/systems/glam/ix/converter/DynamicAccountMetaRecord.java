package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;

record DynamicAccountMetaRecord<A extends ProgramAccounts<A>>(DynamicAccountMeta<A> dynamicAccountMeta,
                                                              int index,
                                                              boolean writable) implements DynamicAccountMeta<A> {

  @Override
  public boolean writable() {
    return writable;
  }

  @Override
  public void setAccount(final AccountMeta[] mappedAccounts, final A programAccounts) {
    final var accountMeta = programAccounts.accountMeta(dynamicAccountMeta, writable);
    mappedAccounts[index] = accountMeta;
  }
}

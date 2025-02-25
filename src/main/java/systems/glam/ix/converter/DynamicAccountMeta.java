package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;

public interface DynamicAccountMeta<A extends ProgramAccounts<A>> {

  static <A extends ProgramAccounts<A>> DynamicAccountMeta<A> create(final DynamicAccountMeta<A> dynamicAccountMeta,
                                                                     int index,
                                                                     boolean writable) {
    return new DynamicAccountMetaRecord<>(dynamicAccountMeta, index, writable);
  }

  boolean writable();

  void setAccount(final AccountMeta[] mappedAccounts, final A programAccounts);
}

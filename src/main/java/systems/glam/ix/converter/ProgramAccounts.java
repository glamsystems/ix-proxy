package systems.glam.ix.converter;

import software.sava.core.accounts.meta.AccountMeta;

public interface ProgramAccounts<A extends ProgramAccounts<A>> {

  AccountMeta accountMeta(final DynamicAccountMeta<A> dynamicAccountMeta, final boolean writable);
}

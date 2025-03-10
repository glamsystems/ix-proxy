package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import systems.comodal.jsoniter.JsonIterator;

import java.util.Map;

public interface IndexedAccountMeta {

  static IndexedAccountMeta parseConfig(final Map<AccountMeta, AccountMeta> accountMetaCache,
                                        final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache,
                                        final JsonIterator ji) {
    final var parser = new IndexedAccountMetaRecord.Parser(accountMetaCache, indexedAccountMetaCache);
    ji.testObject(parser);
    return parser.create();
  }

  static AccountMeta createMeta(final PublicKey account, final boolean writable, final boolean signer) {
    if (signer) {
      return writable
          ? AccountMeta.createWritableSigner(account)
          : AccountMeta.createReadOnlySigner(account);
    } else if (writable) {
      return AccountMeta.createWrite(account);
    } else {
      return AccountMeta.createRead(account);
    }
  }

  void setAccount(final AccountMeta[] accounts);

  AccountMeta accountMeta();

  int index();
}

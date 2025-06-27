package systems.glam.ix.proxy;

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

  void setAccount(final AccountMeta[] accounts);

  AccountMeta accountMeta();

  int index();
}

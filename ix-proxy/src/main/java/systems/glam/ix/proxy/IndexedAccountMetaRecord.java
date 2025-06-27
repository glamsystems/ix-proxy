package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.Map;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;
import static systems.glam.ix.proxy.ProgramMapConfig.PARSE_BASE58_PUBLIC_KEY;

record IndexedAccountMetaRecord(AccountMeta accountMeta, int index) implements IndexedAccountMeta {

  @Override
  public void setAccount(final AccountMeta[] accounts) {
    accounts[index] = accountMeta;
  }

  static final class Parser implements FieldBufferPredicate {

    private final Map<AccountMeta, AccountMeta> accountMetaCache;
    private final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache;

    private PublicKey account;
    private int index;
    private boolean writable;
    private boolean signer;

    Parser(final Map<AccountMeta, AccountMeta> accountMetaCache,
           final Map<IndexedAccountMeta, IndexedAccountMeta> indexedAccountMetaCache) {
      this.accountMetaCache = accountMetaCache;
      this.indexedAccountMetaCache = indexedAccountMetaCache;
    }

    IndexedAccountMeta create() {
      final var accountMeta = AccountMeta.createMeta(account, writable, signer);
      final var cachedMeta = accountMetaCache.putIfAbsent(accountMeta, accountMeta);

      final var indexedMeta = new IndexedAccountMetaRecord(cachedMeta == null ? accountMeta : cachedMeta, index);
      final var cachedIndexedMeta = indexedAccountMetaCache.putIfAbsent(indexedMeta, indexedMeta);

      return cachedIndexedMeta == null ? indexedMeta : cachedIndexedMeta;
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("account", buf, offset, len)) {
        account = ji.applyChars(PARSE_BASE58_PUBLIC_KEY);
      } else if (fieldEquals("index", buf, offset, len)) {
        index = ji.readInt();
      } else if (fieldEquals("writable", buf, offset, len)) {
        writable = ji.readBoolean();
      } else if (fieldEquals("signer", buf, offset, len)) {
        signer = ji.readBoolean();
      } else {
        throw new IllegalStateException("Unknown IndexedAccountMeta field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

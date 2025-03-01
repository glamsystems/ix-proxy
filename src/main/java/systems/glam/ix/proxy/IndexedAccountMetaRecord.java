package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;
import static systems.glam.ix.proxy.ProgramMapConfig.PARSE_BASE58_PUBLIC_KEY;

record IndexedAccountMetaRecord(AccountMeta accountMeta, int index) implements IndexedAccountMeta {

  @Override
  public void setAccount(final AccountMeta[] accounts) {
    accounts[index] = accountMeta;
  }

  static final class Parser implements FieldBufferPredicate {

    private PublicKey account;
    private int index;
    private boolean writable;
    private boolean signer;

    Parser() {
    }

    IndexedAccountMeta create() {
      final var accountMeta = IndexedAccountMeta.createMeta(account, writable, signer);
      return new IndexedAccountMetaRecord(accountMeta, index);
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

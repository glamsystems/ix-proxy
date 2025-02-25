package systems.glam.ix.converter;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record IndexedAccountMeta(AccountMeta accountMeta, int index) {

  public void setAccount(final AccountMeta[] accounts) {
    accounts[index] = accountMeta;
  }

  public static IndexedAccountMeta parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey account;
    private int index;
    private boolean writable;
    private boolean signer;

    private Parser() {
    }

    private IndexedAccountMeta create() {
      final AccountMeta accountMeta;
      if (signer) {
        accountMeta = writable
            ? AccountMeta.createWritableSigner(account)
            : AccountMeta.createReadOnlySigner(account);
      } else if (writable) {
        accountMeta = AccountMeta.createWrite(account);
      } else {
        accountMeta = AccountMeta.createRead(account);
      }
      return new IndexedAccountMeta(accountMeta, index);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("account", buf, offset, len)) {
        account = PublicKey.fromBase58Encoded(ji.readString());
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

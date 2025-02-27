package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;
import static systems.glam.ix.proxy.ProgramMapConfig.PARSE_BASE58_PUBLIC_KEY;

public record IndexedAccountMeta(AccountMeta accountMeta, int index) {

  public void setAccount(final AccountMeta[] accounts) {
    accounts[index] = accountMeta;
  }

  public static IndexedAccountMeta parseConfig(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  public static AccountMeta createMeta(final PublicKey account, final boolean writable, final boolean signer) {
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

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey account;
    private int index;
    private boolean writable;
    private boolean signer;

    private Parser() {
    }

    private IndexedAccountMeta create() {
      final var accountMeta = createMeta(account, writable, signer);
      return new IndexedAccountMeta(accountMeta, index);
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

package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;
import java.util.List;

final class IxProxyRecord<A> extends BaseIxProxy<A> {

  private final Discriminator proxyDiscriminator;
  final List<DynamicAccount<A>> dynamicAccounts;
  final List<IndexedAccountMeta> staticAccounts;
  final int[] indexes;
  private final int numAccounts;
  private final int lengthDelta;

  IxProxyRecord(AccountMeta readCpiProgram,
                AccountMeta invokedProxyProgram,
                Discriminator cpiDiscriminator,
                Discriminator proxyDiscriminator,
                List<DynamicAccount<A>> dynamicAccounts,
                List<IndexedAccountMeta> staticAccounts,
                int[] indexes,
                int numAccounts) {
    super(readCpiProgram, invokedProxyProgram, cpiDiscriminator);
    this.proxyDiscriminator = proxyDiscriminator;
    this.dynamicAccounts = dynamicAccounts;
    this.staticAccounts = staticAccounts;
    this.indexes = indexes;
    this.numAccounts = numAccounts;
    this.lengthDelta = proxyDiscriminator.length() - cpiDiscriminator.length();
  }

  @Override
  public Instruction mapInstruction(final AccountMeta feePayer,
                                    final A runtimeAccounts,
                                    final Instruction instruction) {
    validateMapping(instruction);

    final var accounts = instruction.accounts();
    final int numAccounts = accounts.size();
    final int numExtraAccounts = numAccounts - indexes.length;

    final var mappedAccounts = new AccountMeta[this.numAccounts + numExtraAccounts];
    for (final var dynamicAccount : dynamicAccounts) {
      dynamicAccount.setAccount(mappedAccounts, readCpiProgram, feePayer, runtimeAccounts);
    }
    for (final var staticAccount : staticAccounts) {
      staticAccount.setAccount(mappedAccounts);
    }

    int s = 0;
    int m;
    for (; s < indexes.length; ++s) {
      m = indexes[s];
      if (m >= 0) {
        mappedAccounts[m] = accounts.get(s);
      }
    }

    // Copy extra accounts.
    m = this.numAccounts;
    for (; s < numAccounts; ++s, ++m) {
      mappedAccounts[m] = accounts.get(s);
    }


    final int cpiDataLength = instruction.len();
    final byte[] data = new byte[cpiDataLength + lengthDelta];
    proxyDiscriminator.write(data, 0);

    final int cpiDiscriminatorLength = cpiDiscriminator.length();
    final int len = cpiDataLength - cpiDiscriminatorLength;
    if (len > 0) {
      System.arraycopy(
          instruction.data(), instruction.offset() + cpiDiscriminatorLength,
          data, proxyDiscriminator.length(), len
      );
    }

    return Instruction.createInstruction(
        invokedProxyProgram,
        Arrays.asList(mappedAccounts),
        data
    );
  }

  @Override
  public Discriminator proxyDiscriminator() {
    return proxyDiscriminator;
  }
}

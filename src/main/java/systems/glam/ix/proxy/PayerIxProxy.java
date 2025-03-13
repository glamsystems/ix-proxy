package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;

final class PayerIxProxy<A> extends BaseIxProxy<A> {

  final int payerIndex;

  PayerIxProxy(final Discriminator cpiDiscriminator, final int payerIndex) {
    super(cpiDiscriminator);
    this.payerIndex = payerIndex;
  }

  @Override
  public Instruction mapInstructionUnchecked(final AccountMeta readCpiProgram,
                                             final AccountMeta feePayer,
                                             final A runtimeAccounts,
                                             final Instruction instruction) {
    final var accounts = instruction.accounts();
    if (accounts.size() <= payerIndex) {
      throw new IllegalStateException("Expected at least " + (payerIndex + 1) + " accounts, found " + accounts.size());
    }

    final var payer = accounts.get(payerIndex);
    if (payer.write()
        && payer.signer()
        && payer.publicKey().equals(feePayer.publicKey())) {
      return instruction;
    } else {
      final var accountsArray = accounts.toArray(AccountMeta[]::new);
      accountsArray[payerIndex] = feePayer;
      return Instruction.createInstruction(
          instruction.programId(),
          Arrays.asList(accountsArray),
          instruction.data(), instruction.offset(), instruction.len()
      );
    }
  }

  @Override
  public Discriminator proxyDiscriminator() {
    return cpiDiscriminator;
  }
}

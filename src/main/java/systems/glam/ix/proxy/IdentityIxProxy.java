package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

final class IdentityIxProxy<A> extends BaseIxProxy<A> {

  IdentityIxProxy(final AccountMeta readCpiProgram,
                  final AccountMeta invokedProxyProgram,
                  final Discriminator cpiDiscriminator) {
    super(readCpiProgram, invokedProxyProgram, cpiDiscriminator);
  }

  @Override
  public Instruction mapInstructionUnchecked(final AccountMeta feePayer,
                                             final A runtimeAccounts,
                                             final Instruction instruction) {
    return instruction;
  }

  @Override
  public Discriminator proxyDiscriminator() {
    return cpiDiscriminator;
  }
}

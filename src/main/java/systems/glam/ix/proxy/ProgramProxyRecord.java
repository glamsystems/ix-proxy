package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;

final class ProgramProxyRecord<A> extends BaseProgramProxy<A> implements ProgramProxy<A> {

  private final List<IxProxy<A>> ixProxyList;

  ProgramProxyRecord(final AccountMeta readCpiProgram, final List<IxProxy<A>> ixProxyList) {
    super(readCpiProgram);
    this.ixProxyList = ixProxyList;
  }

  private IxProxy<A> lookupProxy(final byte[] data, final int offset, final int length) {
    for (final var proxy : ixProxyList) {
      if (proxy.matchesCpiDiscriminator(data, offset, length)) {
        return proxy;
      }
    }
    return null;
  }

  @Override
  public IxProxy<A> lookupProxy(final Discriminator discriminator) {
    return lookupProxy(discriminator.data(), 0, discriminator.length());
  }

  @Override
  public IxProxy<A> lookupProxy(final Instruction instruction) {
    final byte[] data = instruction.data();
    final int offset = instruction.offset();
    final int length = instruction.len();
    return lookupProxy(data, offset, length);
  }
}

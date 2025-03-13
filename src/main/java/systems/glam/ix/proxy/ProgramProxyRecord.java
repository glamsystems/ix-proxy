package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.tx.Instruction;

import java.util.List;

final class ProgramProxyRecord<A> extends BaseProgramProxy<A> implements ProgramProxy<A> {

  private final List<IxProxy<A>> ixProxyList;

  ProgramProxyRecord(final AccountMeta readCpiProgram, final List<IxProxy<A>> ixProxyList) {
    super(readCpiProgram);
    this.ixProxyList = ixProxyList;
  }

  @Override
  protected IxProxy<A> lookupProxy(final Instruction instruction) {
    final byte[] instructionData = instruction.data();
    final int offset = instruction.offset();
    final int length = instruction.len();
    for (final var proxy : ixProxyList) {
      if (proxy.matchesCpiDiscriminator(instructionData, offset, length)) {
        return proxy;
      }
    }
    return null;
  }
}

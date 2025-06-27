package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Map;

final class FixedLengthDiscriminatorProgramProxy<A> extends BaseProgramProxy<A> implements ProgramProxy<A> {

  private final int discriminatorLength;
  private final Map<Discriminator, IxProxy<A>> ixProxyMap;

  FixedLengthDiscriminatorProgramProxy(final AccountMeta readCpiProgram,
                                       final int discriminatorLength,
                                       final Map<Discriminator, IxProxy<A>> ixProxyMap) {
    super(readCpiProgram);
    this.discriminatorLength = discriminatorLength;
    this.ixProxyMap = ixProxyMap;
  }

  @Override
  public IxProxy<A> lookupProxy(final Discriminator discriminator) {
    return ixProxyMap.get(discriminator);
  }

  @Override
  public IxProxy<A> lookupProxy(final Instruction instruction) {
    final var discriminator = instruction.wrapDiscriminator(discriminatorLength);
    return lookupProxy(discriminator);
  }
}

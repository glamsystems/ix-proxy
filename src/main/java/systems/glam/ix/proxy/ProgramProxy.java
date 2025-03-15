package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;
import java.util.Map;

public interface ProgramProxy<A> extends IxMapper<A> {

  static <A> ProgramProxy<A> createProxy(final AccountMeta readCpiProgram,
                                         final int discriminatorLength,
                                         final Map<Discriminator, IxProxy<A>> ixProxyMap) {
    return new FixedLengthDiscriminatorProgramProxy<>(readCpiProgram, discriminatorLength, ixProxyMap);
  }

  static <A> ProgramProxy<A> createProxy(final AccountMeta readCpiProgram, final List<IxProxy<A>> ixProxyList) {
    return new ProgramProxyRecord<>(readCpiProgram, ixProxyList);
  }

  IxProxy<A> lookupProxy(final Discriminator discriminator);

  IxProxy<A> lookupProxyOrThrow(final Discriminator discriminator);

  IxProxy<A> lookupProxy(final Instruction instruction);

  IxProxy<A> lookupProxyOrThrow(final Instruction instruction);

  PublicKey cpiProgram();
}

package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.List;
import java.util.Map;

public interface ProgramProxy<A> {

  static <A> ProgramProxy<A> createProxy(final int discriminatorLength,
                                         final Map<Discriminator, IxProxy<A>> ixProxyMap) {
    return new FixedLengthDiscriminatorProgramProxy<>(discriminatorLength, ixProxyMap);
  }

  static <A> ProgramProxy<A> createProxy(final List<IxProxy<A>> ixProxyList) {
    return new ProgramProxyRecord<>(ixProxyList);
  }

  static <A> ProgramProxy<A> createProxy(final Map<PublicKey, ProgramProxy<A>> programProxyMap) {
    return new ProgramProxyMap<>(programProxyMap);
  }

  Instruction apply(final AccountMeta feePayer,
                    final A runtimeAccounts,
                    final Instruction instruction);
}

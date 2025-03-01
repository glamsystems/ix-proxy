package systems.glam.ix.proxy;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.tx.Instruction;

import java.util.Map;

final class ProgramProxyMap<A> implements ProgramProxy<A> {

  private final Map<PublicKey, ProgramProxy<A>> programProxyMap;

  ProgramProxyMap(final Map<PublicKey, ProgramProxy<A>> programProxyMap) {
    this.programProxyMap = programProxyMap;
  }

  @Override
  public Instruction apply(final AccountMeta feePayer, final A runtimeAccounts, final Instruction instruction) {
    final var programProxy = programProxyMap.get(instruction.programId().publicKey());
    // TODO: handle non-proxied programs such as the compute budget program explicitly.
    return programProxy == null ? instruction : programProxy.apply(feePayer, runtimeAccounts, instruction);
  }
}

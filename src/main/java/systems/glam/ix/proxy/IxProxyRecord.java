package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;
import java.util.List;

record IxProxyRecord<A>(Discriminator srcDiscriminator,
                        byte[] srcDiscriminatorBytes,
                        Discriminator dstDiscriminator,
                        List<DynamicAccount<A>> dynamicAccounts,
                        List<IndexedAccountMeta> staticAccounts,
                        int[] indexes,
                        int numAccounts) implements IxProxy<A> {

  @Override
  public Instruction mapInstruction(final AccountMeta invokedProgram,
                                    final AccountMeta feePayer,
                                    final A runtimeAccounts,
                                    final Instruction instruction) {
    final int discriminatorLength = srcDiscriminator.length();
    final int glamDiscriminatorLength = dstDiscriminator.length();
    final int lengthDelta = glamDiscriminatorLength - discriminatorLength;
    final int dataLength = instruction.len();
    final byte[] data;
    if (lengthDelta == 0) {
      data = new byte[dataLength];
      System.arraycopy(instruction.data(), instruction.offset(), data, 0, dataLength);
    } else {
      data = new byte[dataLength + lengthDelta];
      System.arraycopy(
          instruction.data(), instruction.offset() + discriminatorLength,
          data, discriminatorLength, dataLength - discriminatorLength
      );
    }
    dstDiscriminator.write(data, 0);

    final var mappedAccounts = new AccountMeta[numAccounts];
    for (final var programAccountMeta : dynamicAccounts) {
      programAccountMeta.setAccount(mappedAccounts, feePayer, runtimeAccounts);
    }
    for (final var indexedAccountMeta : staticAccounts) {
      indexedAccountMeta.setAccount(mappedAccounts);
    }

    final var accounts = instruction.accounts();
    final int numAccounts = accounts.size();

    int s = 0;
    int g;
    for (; s < indexes.length; ++s) {
      g = indexes[s];
      if (g >= 0) {
        mappedAccounts[g] = accounts.get(s);
      }
    }

    // Copy extra accounts.
    g = numAccounts;
    for (; s < numAccounts; ++s, ++g) {
      mappedAccounts[g] = accounts.get(s);
    }

    return Instruction.createInstruction(
        invokedProgram,
        Arrays.asList(mappedAccounts),
        data
    );
  }

  @Override
  public boolean matchesSrcDiscriminator(final byte[] instructionData, final int offset, final int length) {
    final int discriminatorLength = srcDiscriminatorBytes.length;
    if (discriminatorLength <= length) {
      return Arrays.equals(
          instructionData, offset, offset + discriminatorLength,
          srcDiscriminatorBytes, 0, discriminatorLength
      );
    } else {
      return false;
    }
  }
}

package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

record IxProxyRecord<A>(AccountMeta readCpiProgram,
                        AccountMeta invokedProxyProgram,
                        Discriminator cpiDiscriminator,
                        byte[] cpiDiscriminatorBytes,
                        Discriminator proxyDiscriminator,
                        List<DynamicAccount<A>> dynamicAccounts,
                        List<IndexedAccountMeta> staticAccounts,
                        int[] indexes,
                        int numAccounts) implements IxProxy<A> {

  @Override
  public Instruction mapInstruction(final AccountMeta feePayer,
                                    final A runtimeAccounts,
                                    final Instruction instruction) {
    if (!instruction.programId().publicKey().equals(readCpiProgram.publicKey())) {
      throw new IllegalStateException(String.format("""
              Expected CPI program to be %s, but was %s for invoked proxy program %s.""",
          readCpiProgram.publicKey(), instruction.programId().publicKey(), invokedProxyProgram.publicKey()
      ));
    }

    final int cpiDiscriminatorLength = cpiDiscriminator.length();
    if (cpiDiscriminatorBytes.length != cpiDiscriminatorLength) {
      throw new IllegalStateException(String.format(
          "Expected CPI discriminator length of %d, but was %d.",
          cpiDiscriminatorBytes.length, cpiDiscriminatorLength
      ));
    }

    final int cpiDataLength = instruction.len();
    final int cpiDataOffset = instruction.offset();
    final byte[] cpiData = instruction.data();
    if (!Arrays.equals(
        cpiData, cpiDataOffset, cpiDataOffset + cpiDiscriminatorLength,
        cpiDiscriminatorBytes, 0, cpiDiscriminatorLength
    )) {
      throw new IllegalStateException(String.format(
          "Expected CPI discriminator %s, but was %s.",
          Base64.getEncoder().encodeToString(cpiDiscriminatorBytes),
          Base64.getEncoder().encodeToString(Arrays.copyOfRange(cpiData, cpiDataOffset, cpiDataOffset + cpiDiscriminatorLength))
      ));
    }

    final int proxyDiscriminatorLength = proxyDiscriminator.length();
    final int lengthDelta = proxyDiscriminatorLength - cpiDiscriminatorLength;

    final byte[] data;
    if (lengthDelta == 0) {
      data = new byte[cpiDataLength];
      System.arraycopy(cpiData, cpiDataOffset, data, 0, cpiDataLength);
    } else {
      data = new byte[cpiDataLength + lengthDelta];
      System.arraycopy(
          cpiData, cpiDataOffset + cpiDiscriminatorLength,
          data, cpiDiscriminatorLength, cpiDataLength - cpiDiscriminatorLength
      );
    }
    proxyDiscriminator.write(data, 0);

    final var mappedAccounts = new AccountMeta[numAccounts];
    for (final var programAccountMeta : dynamicAccounts) {
      programAccountMeta.setAccount(mappedAccounts, readCpiProgram, feePayer, runtimeAccounts);
    }
    for (final var staticAccount : staticAccounts) {
      staticAccount.setAccount(mappedAccounts);
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
        invokedProxyProgram,
        Arrays.asList(mappedAccounts),
        data
    );
  }

  @Override
  public boolean matchesCpiDiscriminator(final byte[] instructionData, final int offset, final int length) {
    final int discriminatorLength = cpiDiscriminatorBytes.length;
    if (discriminatorLength <= length) {
      return Arrays.equals(
          instructionData, offset, offset + discriminatorLength,
          cpiDiscriminatorBytes, 0, discriminatorLength
      );
    } else {
      return false;
    }
  }
}

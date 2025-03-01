package systems.glam.ix.proxy;

import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

record IxProxyRecord<A>(AccountMeta readCpiProgram,
                        AccountMeta invokedProxyProgram,
                        Discriminator srcDiscriminator,
                        byte[] srcDiscriminatorBytes,
                        Discriminator dstDiscriminator,
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

    final int srcDiscriminatorLength = srcDiscriminator.length();
    if (srcDiscriminatorBytes.length != srcDiscriminatorLength) {
      throw new IllegalStateException(String.format(
          "Expected src discriminator length of %d, but was %d.",
          srcDiscriminatorBytes.length, srcDiscriminatorLength
      ));
    }
    final int srcDataLength = instruction.len();
    final int srcDataOffset = instruction.offset();
    final byte[] srcData = instruction.data();
    if (!Arrays.equals(
        srcData, srcDataOffset, srcDataOffset + srcDiscriminatorLength,
        srcDiscriminatorBytes, 0, srcDiscriminatorLength
    )) {
      throw new IllegalStateException(String.format(
          "Expected src discriminator %s, but was %s.",
          Base64.getEncoder().encodeToString(srcDiscriminatorBytes),
          Base64.getEncoder().encodeToString(Arrays.copyOfRange(srcData, srcDataOffset, srcDataOffset + srcDiscriminatorLength))
      ));
    }

    final int dstDiscriminatorLength = dstDiscriminator.length();
    final int lengthDelta = dstDiscriminatorLength - srcDiscriminatorLength;

    final byte[] data;
    if (lengthDelta == 0) {
      data = new byte[srcDataLength];
      System.arraycopy(srcData, srcDataOffset, data, 0, srcDataLength);
    } else {
      data = new byte[srcDataLength + lengthDelta];
      System.arraycopy(
          srcData, srcDataOffset + srcDiscriminatorLength,
          data, srcDiscriminatorLength, srcDataLength - srcDiscriminatorLength
      );
    }
    dstDiscriminator.write(data, 0);

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

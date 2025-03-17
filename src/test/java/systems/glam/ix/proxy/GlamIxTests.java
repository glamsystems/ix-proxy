package systems.glam.ix.proxy;

import org.junit.jupiter.api.Test;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.SolanaAccounts;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.encoding.ByteUtil;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class GlamIxTests {

  private static final PublicKey INVOKED_PROGRAM = PublicKey.fromBase58Encoded("GLAMbTqav9N9witRjswJ8enwp9vv5G8bsSJ2kPJ4rcyc");

  record GlamVaultAccounts(AccountMeta readGlamState,
                           AccountMeta writeGlamState,
                           AccountMeta readGlamVault,
                           AccountMeta writeGlamVault) {

    static GlamVaultAccounts createAccounts(final PublicKey stateAccount, final PublicKey vaultAccount) {
      return new GlamVaultAccounts(
          AccountMeta.createRead(stateAccount),
          AccountMeta.createWrite(stateAccount),
          AccountMeta.createRead(vaultAccount),
          AccountMeta.createWrite(vaultAccount)
      );
    }
  }

  private static Collection<ProgramProxy<GlamVaultAccounts>> createProxies(final byte[] mappingJson) {
    final var ji = JsonIterator.parse(mappingJson);

    final var accountMetaCache = new HashMap<AccountMeta, AccountMeta>();
    final var indexedAccountMetaCache = new HashMap<IndexedAccountMeta, IndexedAccountMeta>();

    final var programMapConfig = ProgramMapConfig.parseConfig(accountMetaCache, indexedAccountMetaCache, ji);

    final Function<DynamicAccountConfig, DynamicAccount<GlamVaultAccounts>> dynamicAccountFactory = accountConfig -> {
      final int index = accountConfig.index();
      final boolean w = accountConfig.writable();
      return switch (accountConfig.name()) {
        case "glam_state" -> (mappedAccounts, _, _, vaultAccounts) -> mappedAccounts[index] = w
            ? vaultAccounts.writeGlamState() : vaultAccounts.readGlamState();
        case "glam_vault" -> (mappedAccounts, _, _, vaultAccounts) -> mappedAccounts[index] = w
            ? vaultAccounts.writeGlamVault() : vaultAccounts.readGlamVault();
        case "glam_signer" -> accountConfig.createFeePayerAccount();
        case "cpi_program" -> accountConfig.createReadCpiProgram();
        default -> throw new IllegalStateException("Unknown dynamic account type: " + accountConfig.name());
      };
    };
    return programMapConfig.createProgramProxies(AccountMeta.createInvoked(GlamIxTests.INVOKED_PROGRAM), dynamicAccountFactory);
  }

  private static void createProxies(final Path mappingFile,
                                    final Map<PublicKey, ProgramProxy<GlamVaultAccounts>> programProxies) {
    try {
      final var mappingJson = Files.readAllBytes(mappingFile);
      final var systemProgramProxies = createProxies(mappingJson);
      for (final var proxy : systemProgramProxies) {
        programProxies.put(proxy.cpiProgram(), proxy);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static TransactionMapper<GlamVaultAccounts> createMapper() {
    final var programProxies = new HashMap<PublicKey, ProgramProxy<GlamVaultAccounts>>();
    try (final var paths = Files.walk(Path.of(".mappings"), 1)) {
      paths
          .filter(Files::isRegularFile)
          .filter(Files::isReadable)
          .filter(f -> f.getFileName().toString().endsWith(".json"))
          .forEach(mappingFile -> createProxies(mappingFile, programProxies));
      return TransactionMapper.createMapper(INVOKED_PROGRAM, programProxies);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final TransactionMapper<GlamVaultAccounts> txMapper = createMapper();

  @Test
  void testPayerProxy() throws IOException {
    final var mappingJson = Files.readAllBytes(Path.of(".mappings/ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL.json"));

    final var programProxies = createProxies(mappingJson);
    assertEquals(1, programProxies.size());
    final var programProxy = programProxies.iterator().next();

    var ixProxy = programProxy.lookupProxy(Discriminator.toDiscriminator(0));
    assertNotNull(ixProxy);
    assertEquals(ixProxy.proxyDiscriminator(), ixProxy.cpiDiscriminator());

    assertInstanceOf(PayerIxProxy.class, ixProxy);
    var payerProxy = (PayerIxProxy<GlamVaultAccounts>) ixProxy;
    assertEquals(0, payerProxy.payerIndex);
  }

  @Test
  void testGlamDriftRemapping() throws IOException {
    final var mappingJson = Files.readAllBytes(Path.of(".mappings/dRiftyHA39MWEi3m9aunc5MzRF1JYuBsbn6VPcn33UH.json"));

    final var programProxies = createProxies(mappingJson);
    assertEquals(1, programProxies.size());
    final var programProxy = programProxies.iterator().next();

    var ixProxy = programProxy.lookupProxy(Discriminator.toDiscriminator(186, 85, 17, 249, 219, 231, 98, 251));
    assertNotNull(ixProxy);

    assertEquals(
        Discriminator.toDiscriminator(179, 118, 20, 212, 145, 146, 49, 130),
        ixProxy.proxyDiscriminator()
    );

    assertInstanceOf(IxProxyRecord.class, ixProxy);
    final var ixProxyRecord = (IxProxyRecord<GlamVaultAccounts>) ixProxy;

    var dynamicAccounts = ixProxyRecord.dynamicAccounts;
    assertEquals(4, dynamicAccounts.size());

    var staticAccounts = ixProxyRecord.staticAccounts;
    assertEquals(0, staticAccounts.size());

    assertArrayEquals(
        new int[]{4, 5, 6, -1},
        ixProxyRecord.indexes
    );
  }

  private static void validateMappedIx(final Instruction sourceIx,
                                       final Discriminator sourceDiscriminator,
                                       final Instruction mappedIx,
                                       final Discriminator proxyDiscriminator) {
    assertEquals(INVOKED_PROGRAM, mappedIx.programId().publicKey());

    final int srcDiscriminatorLen = sourceDiscriminator.length();
    assertEquals(sourceDiscriminator, sourceIx.wrapDiscriminator(srcDiscriminatorLen));
    final int proxyDiscriminatorLen = proxyDiscriminator.length();
    assertEquals(proxyDiscriminator, mappedIx.wrapDiscriminator(proxyDiscriminatorLen));

    final var sourceData = sourceIx.data();
    final var mappedData = mappedIx.data();
    assertEquals(proxyDiscriminatorLen - srcDiscriminatorLen, mappedData.length - sourceData.length);

    assertArrayEquals(
        Arrays.copyOfRange(sourceData, srcDiscriminatorLen, sourceData.length),
        Arrays.copyOfRange(mappedData, proxyDiscriminatorLen, mappedData.length)
    );
  }

  @Test
  void testGlamDriftDeposit() {
    final var srcIxData = """
        [
          {
            "programId": "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
            "accounts": [
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": false,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "So11111111111111111111111111111111111111112",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "11111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              }
            ],
            "data": "AQ=="
          },
          {
            "programId": "11111111111111111111111111111111",
            "accounts": [
              {
                "publicKey": "4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd",
                "feePayer": false,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "AgAAAADh9QUAAAAA"
          },
          {
            "programId": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            "accounts": [
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "EQ=="
          },
          {
            "programId": "dRiftyHA39MWEi3m9aunc5MzRF1JYuBsbn6VPcn33UH",
            "accounts": [
              {
                "publicKey": "5zpq7DvB6UdFFvpmBPspGPNfUGoBRRCE2HHg5u3gxcsN",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "CTESGrMN2o91gtyaawyuHrGPbpjuyF1AcGqQF638wWf9",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "6kDHCuUZMmNarhbWjGNekn9G1UgT4yJHh39pwCiAY8Ff",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd",
                "feePayer": false,
                "signer": true,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "DfYCNezifxAEsQbAJ1b3j6PX3JVBe8fu11KBhxsbw5d2",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "3m6i4RFWEDw2Ft4tFHPJtYgmpPe21k56M3FHeWYrgGBz",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "3x85u7SWkmmr7YQGYhtjARgxwegTLJgkSLRprfXod6rh",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "8iPGiVLh8rYBAADh9QUAAAAAAA=="
          }
        ]
        """;

    final var mappedIxData = """
        [
          {
            "programId": "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
            "accounts": [
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": false,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "So11111111111111111111111111111111111111112",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "11111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              }
            ],
            "data": "AQ=="
          },
          {
            "programId": "GLAMbTqav9N9witRjswJ8enwp9vv5G8bsSJ2kPJ4rcyc",
            "accounts": [
              {
                "publicKey": "5X1AWem3eTtXRFFh9PMSPZmZfcdExXHD25cJXnvxrzTy",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": true,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "11111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "p6TDm9uYv+YA4fUFAAAAAA=="
          },
          {
            "programId": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            "accounts": [
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "EQ=="
          },
          {
            "programId": "GLAMbTqav9N9witRjswJ8enwp9vv5G8bsSJ2kPJ4rcyc",
            "accounts": [
              {
                "publicKey": "5X1AWem3eTtXRFFh9PMSPZmZfcdExXHD25cJXnvxrzTy",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": true,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "dRiftyHA39MWEi3m9aunc5MzRF1JYuBsbn6VPcn33UH",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "5zpq7DvB6UdFFvpmBPspGPNfUGoBRRCE2HHg5u3gxcsN",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "CTESGrMN2o91gtyaawyuHrGPbpjuyF1AcGqQF638wWf9",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "6kDHCuUZMmNarhbWjGNekn9G1UgT4yJHh39pwCiAY8Ff",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "DfYCNezifxAEsQbAJ1b3j6PX3JVBe8fu11KBhxsbw5d2",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "GmWA1scwdESnC7PxitTJ8nNkYL4qf5JkRskc8hZ7ZFwE",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "3m6i4RFWEDw2Ft4tFHPJtYgmpPe21k56M3FHeWYrgGBz",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "3x85u7SWkmmr7YQGYhtjARgxwegTLJgkSLRprfXod6rh",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "/D/6yWI3ggwBAADh9QUAAAAAAA=="
          }
        ]
        """;

    final var solanaAccounts = SolanaAccounts.MAIN_NET;
    final var driftProgram = PublicKey.fromBase58Encoded("dRiftyHA39MWEi3m9aunc5MzRF1JYuBsbn6VPcn33UH");

    final var srcInstructions = parseInstructions(srcIxData);
    assertEquals(4, srcInstructions.size());

    var createTokenAccountIx = srcInstructions.getFirst();
    assertEquals(solanaAccounts.associatedTokenAccountProgram(), createTokenAccountIx.programId().publicKey());
    // CreateIdempotent
    assertEquals(1, createTokenAccountIx.data()[0]);

    var systemTransferIx = srcInstructions.get(1);
    assertEquals(solanaAccounts.systemProgram(), systemTransferIx.programId().publicKey());
    // Transfer
    assertEquals(2, ByteUtil.getInt32LE(systemTransferIx.data(), 0));

    var syncNativeIx = srcInstructions.get(2);
    assertEquals(solanaAccounts.tokenProgram(), syncNativeIx.programId().publicKey());
    // SyncNative
    assertEquals(17, syncNativeIx.data()[0]);

    var depositIx = srcInstructions.getLast();
    assertEquals(driftProgram, depositIx.programId().publicKey());

    final var stateAccount = PublicKey.fromBase58Encoded("5X1AWem3eTtXRFFh9PMSPZmZfcdExXHD25cJXnvxrzTy");
    final var vaultAccount = PublicKey.fromBase58Encoded("4Uug1zHGvkYFuTGvS34Q5MnP7Bf36paXjtU6REUZnMhd");
    final var vaultAccounts = GlamVaultAccounts.createAccounts(stateAccount, vaultAccount);
    final var feePayer = AccountMeta.createFeePayer(PublicKey.fromBase58Encoded("AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD"));

    final var expectedInstructions = parseInstructions(mappedIxData);
    assertEquals(4, expectedInstructions.size());
    // No mapping
    assertEquals(createTokenAccountIx, expectedInstructions.getFirst());
    assertEquals(syncNativeIx, srcInstructions.get(2));

    final var mappedInstructions = txMapper.mapInstructions(feePayer, vaultAccounts, srcInstructions);
    assertEquals(4, mappedInstructions.length);
    // No mapping
    assertEquals(createTokenAccountIx, mappedInstructions[0]);
    assertEquals(syncNativeIx, mappedInstructions[2]);

    final var mappedTransferIx = mappedInstructions[1];
    validateMappedIx(
        systemTransferIx, Discriminator.toDiscriminator(2, 0, 0, 0),
        mappedTransferIx, Discriminator.toDiscriminator(167, 164, 195, 155, 219, 152, 191, 230)
    );

    final var mappedDepositIx = mappedInstructions[3];
    validateMappedIx(
        depositIx, Discriminator.toDiscriminator(242, 35, 198, 137, 82, 225, 242, 182),
        mappedDepositIx, Discriminator.toDiscriminator(252, 63, 250, 201, 98, 55, 130, 12)
    );

    assertEquals(expectedInstructions, Arrays.asList(mappedInstructions));
  }

  @Test
  void testGlamMarinadeUnstake() {
    final var srcIxData = """
        [{
          "programId": "11111111111111111111111111111111",
          "accounts": [{
              "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
              "feePayer": false,
              "signer": true,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            }],
          "data": "AwAAAI4hVXVdrV4a/mWQikafyFE8XxSgQvDuF+bnvW+exKlqHAAAAAAAAAAyMDI1LTAzLTE0VDIxOjIzOjUyLjgyODM2MFp+gPAWAAAAAABYAAAAAAAAAAVF42W+8nGtdTUDZ1ZdpA2jNtwch5uxVIp6/MVaqTke"
        },
        {
          "programId": "MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD",
          "accounts":     [{
              "publicKey": "8szGkuLTAux9XMgZ2vtY39jVSowEcpBfFfD8hXSEqdGC",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "mSoLzYCxHdYgdzU16g5QSh3i5K3z3KZK7ytfqcJm7So",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "Hk5Js6PRDejQwWk164smpPtfymoKwP6iDjogYKozffhG",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "9AmBGCiDdiHSA7mspgFa6pwZ1PTfBj7DhF6nudpiXmNN",
              "feePayer": false,
              "signer": true,
              "writable": false,
              "invoked": false
            },{
              "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "SysvarC1ock11111111111111111111111111111111",
              "feePayer": false,
              "signer": false,
              "writable": false,
              "invoked": false
            },{
              "publicKey": "SysvarRent111111111111111111111111111111111",
              "feePayer": false,
              "signer": false,
              "writable": false,
              "invoked": false
            },{
              "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
              "feePayer": false,
              "signer": false,
              "writable": false,
              "invoked": false
            }],
          "data": "YaeQa3W+gCQA4fUFAAAAAA=="
        }]""";

    final var mappedIxData = """
        [
          {
            "programId": "11111111111111111111111111111111",
            "accounts": [
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": false,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "AwAAAI4hVXVdrV4a/mWQikafyFE8XxSgQvDuF+bnvW+exKlqHAAAAAAAAAAyMDI1LTAzLTE0VDIxOjIzOjUyLjgyODM2MFp+gPAWAAAAAABYAAAAAAAAAAVF42W+8nGtdTUDZ1ZdpA2jNtwch5uxVIp6/MVaqTke"
          },
          {
            "programId": "GLAMbTqav9N9witRjswJ8enwp9vv5G8bsSJ2kPJ4rcyc",
            "accounts": [
              {
                "publicKey": "C8QQdekKRFRt5u78vo3YQcZTLWB5WkPhzpkwsKwjnNTo",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "9AmBGCiDdiHSA7mspgFa6pwZ1PTfBj7DhF6nudpiXmNN",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": true,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "8szGkuLTAux9XMgZ2vtY39jVSowEcpBfFfD8hXSEqdGC",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "mSoLzYCxHdYgdzU16g5QSh3i5K3z3KZK7ytfqcJm7So",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "Hk5Js6PRDejQwWk164smpPtfymoKwP6iDjogYKozffhG",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "SysvarC1ock11111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "SysvarRent111111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              }
            ],
            "data": "ygMhG7ecOecA4fUFAAAAAA=="
          }
        ]
        """;

    final var srcInstructions = parseInstructions(srcIxData);
    assertEquals(2, srcInstructions.size());
    final var createTicketIx = srcInstructions.getFirst();
    final var marinadeProgram = PublicKey.fromBase58Encoded("MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD");
    final var unstakeIx = srcInstructions.getLast();
    assertEquals(marinadeProgram, unstakeIx.programId().publicKey());

    final var stateAccount = PublicKey.fromBase58Encoded("C8QQdekKRFRt5u78vo3YQcZTLWB5WkPhzpkwsKwjnNTo");
    final var vaultAccount = PublicKey.fromBase58Encoded("9AmBGCiDdiHSA7mspgFa6pwZ1PTfBj7DhF6nudpiXmNN");
    final var vaultAccounts = GlamVaultAccounts.createAccounts(stateAccount, vaultAccount);
    final var feePayer = AccountMeta.createFeePayer(PublicKey.fromBase58Encoded("AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD"));

    final var expectedInstructions = parseInstructions(mappedIxData);
    assertEquals(2, expectedInstructions.size());
    // no mapping
    assertEquals(createTicketIx, expectedInstructions.getFirst());

    final var mappedInstructions = txMapper.mapInstructions(feePayer, vaultAccounts, srcInstructions);
    assertEquals(2, mappedInstructions.length);
    // no mapping
    assertEquals(createTicketIx, mappedInstructions[0]);

    final var mappedUnstakeIx = mappedInstructions[1];
    validateMappedIx(
        unstakeIx, Discriminator.toDiscriminator(97, 167, 144, 107, 117, 190, 128, 36),
        mappedUnstakeIx, Discriminator.toDiscriminator(202, 3, 33, 27, 183, 156, 57, 231)
    );

    assertEquals(expectedInstructions, Arrays.asList(mappedInstructions));
  }

  private static List<Instruction> parseInstructions(final String ixData) {
    final var instructions = new ArrayList<Instruction>();
    final var ji = JsonIterator.parse(ixData);
    while (ji.readArray()) {
      final var programId = PublicKey.fromBase58Encoded(ji.skipUntil("programId").readString());
      final var accounts = new ArrayList<AccountMeta>();
      for (ji.skipUntil("accounts"); ji.readArray(); ) {
        final var parser = new AccountMetaParser();
        ji.testObject(parser);
        accounts.add(parser.createMeta());
      }
      final byte[] data = ji.skipUntil("data").decodeBase64String();
      final var ix = Instruction.createInstruction(programId, accounts, data);
      instructions.add(ix);
      ji.closeObj();
    }
    return instructions;
  }

  static final class AccountMetaParser implements FieldBufferPredicate {

    private PublicKey publicKey;
    private boolean feePayer;
    private boolean signer;
    private boolean writable;
    private boolean invoked;

    private AccountMetaParser() {
    }

    AccountMeta createMeta() {
      return AccountMeta.createMeta(
          publicKey,
          invoked,
          feePayer,
          writable,
          signer
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("publicKey", buf, offset, len)) {
        publicKey = PublicKey.fromBase58Encoded(ji.readString());
      } else if (fieldEquals("feePayer", buf, offset, len)) {
        feePayer = ji.readBoolean();
      } else if (fieldEquals("signer", buf, offset, len)) {
        signer = ji.readBoolean();
      } else if (fieldEquals("writable", buf, offset, len)) {
        writable = ji.readBoolean();
      } else if (fieldEquals("invoked", buf, offset, len)) {
        invoked = ji.readBoolean();
      } else {
        throw new IllegalStateException("Unexpected field: " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

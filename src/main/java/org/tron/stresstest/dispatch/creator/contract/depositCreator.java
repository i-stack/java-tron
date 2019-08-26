package org.tron.stresstest.dispatch.creator.contract;

import static org.tron.core.Wallet.addressValid;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;

@Slf4j
@Setter
public class depositCreator extends AbstractTransactionCreator implements
    GoodCaseTransactonCreator {

  private String ownerAddress = WithdrawToAddress;
  private String contractAddress = MainGatewayContractAddress;
  private long callValue = 1L;
  private String methodSign = "depositTRX()";
  private boolean hex = false;
  private String param = "";
  private long feeLimit = 1000000000L;
  private String privateKey = WithdrawToPrivateKey;
  public static AtomicLong queryCount = new AtomicLong();

  @Override
  protected Protocol.Transaction create() {

    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;
    try {
      contract = triggerCallContract(
          ownerAddressBytes,
          Wallet.decodeFromBase58Check(contractAddress),
//              contractAddress.getBytes(),
          callValue,
          Hex.decode(AbiUtil.parseMethod(
              methodSign,
              param,
              hex
          )));
    } catch (EncodingException e) {
      e.printStackTrace();
    }

    Protocol.Transaction transaction = createTransaction(contract,
        ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder()
        .setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();
//    String mainGateWay = "TYYrjz9W9ii98zMEF7KoL24KhGRXqWpjEJ";
    //transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)),
    //transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(addressAndPri[1])),
    //    decodeFromBase58Check(mainGateWay), false);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
//    String fullnode = "39.106.90.236:50151";
//    Channel channelFull = ManagedChannelBuilder.forTarget(fullnode)
//        .usePlaintext(true)
//        .build();
//    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
//    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    return transaction;
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length]
        && hash1[1] == decodeCheck[decodeData.length + 1]
        && hash1[2] == decodeCheck[decodeData.length + 2]
        && hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  /**
   * constructor.
   */
  public static Transaction sign(Transaction transaction, ECKey myKey, byte[] chainId,
      boolean isMainChain) {
    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());

    byte[] newHash;
    if (isMainChain) {
      newHash = hash;
    } else {
      byte[] hashWithChainId = Arrays.copyOf(hash, hash.length + chainId.length);
      System.arraycopy(chainId, 0, hashWithChainId, hash.length, chainId.length);
      newHash = Sha256Hash.hash(hashWithChainId);
    }

    ECDSASignature signature = myKey.sign(newHash);
    ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
    transactionBuilderSigned.addSignature(bsSign);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

}
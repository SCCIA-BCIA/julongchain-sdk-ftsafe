/**
 * Copyright DingXuan. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bcia.julongchain.consenter.common.blockcutter;

import org.apache.commons.lang3.ArrayUtils;
import org.bcia.julongchain.common.groupconfig.config.IConsenterConfig;
import org.bcia.julongchain.common.log.JavaChainLog;
import org.bcia.julongchain.common.log.JavaChainLogFactory;
import org.bcia.julongchain.consenter.consensus.IReceiver;
import org.bcia.julongchain.consenter.entity.BatchesMes;
import org.bcia.julongchain.protos.common.Common;
//import org.springframework.stereotype.Component;

/**
 * @author zhangmingyang
 * @Date: 2018/3/15
 * @company Dingxuan
 */
//@Component
public class BlockCutter implements IReceiver {
    private static JavaChainLog log = JavaChainLogFactory.getLog(BlockCutter.class);

    private IConsenterConfig sharedConfigManager;

    private Common.Envelope[] pendingBatch;

    private int pendingBatchSizeBytes;


    public BlockCutter() {
    }

    public BlockCutter(IConsenterConfig sharedConfigManager) {
        this.sharedConfigManager = sharedConfigManager;
    }

    @Override
    public BatchesMes ordered(Common.Envelope msg) {

        int messageSizeBytes = messageSizeBytes(msg);
        BatchesMes batchesMes = new BatchesMes();
        if (messageSizeBytes > sharedConfigManager.getBatchSize().getPreferredMaxBytes()) {

            log.debug(String.format("he current message, with %v bytes, is larger than the preferred batch size of %v bytes and will be isolated.", messageSizeBytes, sharedConfigManager.getBatchSize().getPreferredMaxBytes()));

            if (pendingBatch.length > 0) {
                Common.Envelope[] messageBatch = cut();
                batchesMes.setMessageBatches((Common.Envelope[][]) ArrayUtils.add(batchesMes.getMessageBatches(), messageBatch));
                //batchesMes.messageBatches = (Common.Envelope[][]) ArrayUtils.add(batchesMes.messageBatches, messageBatch);
            }
            batchesMes.setMessageBatches((Common.Envelope[][]) ArrayUtils.add(batchesMes.getMessageBatches(), new Common.Envelope[]{msg}));
          //  batchesMes.messageBatches = (Common.Envelope[][]) ArrayUtils.add(batchesMes.messageBatches, new Common.Envelope[]{msg});
        }

        boolean messageWillOverflowBatchSizeBytes = pendingBatchSizeBytes +messageSizeBytes> sharedConfigManager.getBatchSize().getPreferredMaxBytes();
        if (messageWillOverflowBatchSizeBytes) {
            log.debug(String.format("The current message, with %v bytes, will overflow the pending batch of %v bytes.", messageSizeBytes, pendingBatchSizeBytes));
            log.debug("Pending batch would overflow if current message is added, cutting batch now.");
            Common.Envelope[] messageBatch = cut();
           // batchesMes.messageBatches = (Common.Envelope[][]) ArrayUtils.add(batchesMes.getMessageBatches(), messageBatch);
            batchesMes.setMessageBatches((Common.Envelope[][]) ArrayUtils.add(batchesMes.getMessageBatches(), messageBatch));
        }
        log.debug("Enqueuing message into batch");

        pendingBatch = (Common.Envelope[]) ArrayUtils.add(pendingBatch, msg);
        pendingBatchSizeBytes += messageSizeBytes;

        batchesMes.setPending(true);
        //batchesMes.pending = true;
        //BatchesMes mes = new BatchesMes();

        if(pendingBatch.length>=sharedConfigManager.getBatchSize().getMaxMessageCount()){
            log.debug("Batch size met,cutting batch");
            Common.Envelope[] messageBatch = cut();
            batchesMes.setMessageBatches((Common.Envelope[][]) ArrayUtils.add(batchesMes.getMessageBatches(),messageBatch));
          //  batchesMes.messageBatches= (Common.Envelope[][]) ArrayUtils.add(batchesMes.messageBatches,messageBatch);
            batchesMes.setPending(true);
          //  batchesMes.pending=true;
        }
        return batchesMes;
    }

    @Override
    public Common.Envelope[] cut() {
        log.info("this is blockCutter'cut method!!");
        Common.Envelope[] batch = pendingBatch;
        this.pendingBatch = null;
        this.pendingBatchSizeBytes = 0;
        return batch;
    }


    private static int messageSizeBytes(Common.Envelope message) {
        return message.getPayload().size() + message.getSignature().size();
    }

    public IConsenterConfig getSharedConfigManager() {
        return sharedConfigManager;
    }

    public void setSharedConfigManager(IConsenterConfig sharedConfigManager) {
        this.sharedConfigManager = sharedConfigManager;
    }

    public Common.Envelope[] getPendingBatch() {
        return pendingBatch;
    }

    public void setPendingBatch(Common.Envelope[] pendingBatch) {
        this.pendingBatch = pendingBatch;
    }

    public int getPendingBatchSizeBytes() {
        return pendingBatchSizeBytes;
    }

    public void setPendingBatchSizeBytes(int pendingBatchSizeBytes) {
        this.pendingBatchSizeBytes = pendingBatchSizeBytes;
    }
}

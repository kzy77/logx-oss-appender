package org.logx.storage.sf;

/**
 * 分片ETag
 * <p>
 * 用于表示分片上传中每个分片的ETag信息。
 */
public class PartETag {
    private final int partNumber;
    private final String eTag;

    /**
     * 构造分片ETag
     *
     * @param partNumber 分片编号
     * @param eTag ETag值
     */
    public PartETag(int partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

    /**
     * 获取分片编号
     *
     * @return 分片编号
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * 获取ETag值
     *
     * @return ETag值
     */
    public String getETag() {
        return eTag;
    }
}
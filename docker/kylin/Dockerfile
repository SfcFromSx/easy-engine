# 在官方 standalone 镜像上只做最小修补：让「首次初始化」标记与 HDFS 数据落在可持久化的 /data 上，
# 并避免每次启动清空 ZooKeeper 数据目录（与持久化 HDFS 一致）。
FROM apachekylin/apache-kylin-standalone:4.0.0

USER root

RUN sed -i 's|/home/admin/first_run|/data/.kylin_first_run|g' /home/admin/entrypoint.sh \
    && sed -i 's/^rm -rf \/data\/zookeeper\/\*$/# &/' /home/admin/entrypoint.sh

USER root

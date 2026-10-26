package com.atsea.zerox.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.atsea.zerox.genresult.entity.ChatHistory;
import com.atsea.zerox.mapper.ChatHistoryMapper;
import com.atsea.zerox.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/AtseazzZ">作者Atsea_zzZ</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}

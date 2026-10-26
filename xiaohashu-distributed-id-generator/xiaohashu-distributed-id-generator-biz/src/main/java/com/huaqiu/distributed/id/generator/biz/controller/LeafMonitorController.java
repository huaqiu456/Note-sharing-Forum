package com.huaqiu.distributed.id.generator.biz.controller;
import com.huaqiu.distributed.id.generator.biz.core.segment.SegmentIDGenImpl;
import com.huaqiu.distributed.id.generator.biz.core.segment.model.LeafAlloc;
import com.huaqiu.distributed.id.generator.biz.core.segment.model.SegmentBuffer;
import com.huaqiu.distributed.id.generator.biz.model.SegmentBufferView;
import com.huaqiu.distributed.id.generator.biz.service.SegmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LeafMonitorController {
    private Logger logger = LoggerFactory.getLogger(LeafMonitorController.class);

    @Autowired
    private SegmentService segmentService;

    @RequestMapping(value = "cache")
    public String getCache(Model model) {
        Map<String, SegmentBufferView> data = new HashMap<>();
        SegmentIDGenImpl segmentIDGen = segmentService.getIdGen();
        if (segmentIDGen == null) {
            throw new IllegalArgumentException("You should config leaf.segment.enable=true first");
        }
        Map<String, SegmentBuffer> cache = segmentIDGen.getCache();
        for (Map.Entry<String, SegmentBuffer> entry : cache.entrySet()) {
            SegmentBufferView sv = new SegmentBufferView();
            SegmentBuffer buffer = entry.getValue();
            sv.setInitOk(buffer.isInitOk());
            sv.setKey(buffer.getKey());
            sv.setPos(buffer.getCurrentPos());
            sv.setNextReady(buffer.isNextReady());
            sv.setMax0(buffer.getSegments()[0].getMax());
            sv.setValue0(buffer.getSegments()[0].getValue().get());
            sv.setStep0(buffer.getSegments()[0].getStep());

            sv.setMax1(buffer.getSegments()[1].getMax());
            sv.setValue1(buffer.getSegments()[1].getValue().get());
            sv.setStep1(buffer.getSegments()[1].getStep());

            data.put(entry.getKey(), sv);

        }
        logger.info("Cache info {}", data);
        model.addAttribute("data", data);
        return "segment";
    }

    @RequestMapping(value = "db")
    public String getDb(Model model) {
        SegmentIDGenImpl segmentIDGen = segmentService.getIdGen();
        if (segmentIDGen == null) {
            throw new IllegalArgumentException("You should config leaf.segment.enable=true first");
        }
        List<LeafAlloc> items = segmentIDGen.getAllLeafAllocs();
        logger.info("DB info {}", items);
        model.addAttribute("items", items);
        return "db";
    }

}

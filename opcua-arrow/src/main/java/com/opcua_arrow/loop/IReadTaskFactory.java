package com.opcua_arrow.loop;

import com.opcua_arrow.data.DataReadGroup;

public interface IReadTaskFactory {
    IReaderTask createReader(DataReadGroup readGroup);
}

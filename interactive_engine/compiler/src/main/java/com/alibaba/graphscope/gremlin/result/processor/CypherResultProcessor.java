/*
 * Copyright 2020 Alibaba Group Holding Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.graphscope.gremlin.result.processor;

import com.alibaba.graphscope.common.ir.tools.AliasInference;
import com.alibaba.graphscope.common.ir.tools.GraphPlanner;
import com.alibaba.graphscope.common.ir.tools.LogicalPlan;
import com.alibaba.graphscope.gremlin.result.parser.CypherResultParser;
import com.google.common.collect.Lists;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rel.type.StructKind;
import org.apache.tinkerpop.gremlin.server.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CypherResultProcessor extends AbstractResultProcessor {

    public CypherResultProcessor(Context context, GraphPlanner.Summary summary) {
        super(context, new CypherResultParser(getOutputType(summary.getLogicalPlan())));
    }

    @Override
    protected void aggregateResults() {
        // do nothing
    }

    private static RelDataType getOutputType(LogicalPlan logicalPlan) {
        if (logicalPlan.getRegularQuery() != null) {
            List<RelNode> inputs = Lists.newArrayList(logicalPlan.getRegularQuery());
            List<RelDataTypeField> outputFields = new ArrayList<>();
            while (!inputs.isEmpty()) {
                RelNode cur = inputs.remove(0);
                outputFields.addAll(cur.getRowType().getFieldList());
                if (AliasInference.removeAlias(cur)) {
                    break;
                }
                inputs.addAll(cur.getInputs());
            }
            return new RelRecordType(
                    StructKind.FULLY_QUALIFIED,
                    outputFields.stream().distinct().collect(Collectors.toList()));
        } else {
            return logicalPlan.getProcedureCall().getType();
        }
    }
}

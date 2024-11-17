package io.jenkins.plugins.dumasd.k8s;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameContentConfig;
import io.jenkins.plugins.dumasd.k8s.model.K8sResourceType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
@Log
@Getter
public class KubeResourceAlterParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = 2748319918452193256L;

    private Map<String, List<K8sResourceNameContentConfig>> itemsMap;

    @DataBoundConstructor
    public KubeResourceAlterParameterDefinition(
            @NonNull String name, @NonNull Map<String, List<K8sResourceNameContentConfig>> itemsMap) {
        super(name);
        this.itemsMap = itemsMap;
    }

    public Set<String> getKeys() {
        return itemsMap.keySet();
    }

    public int getKeySelectSize() {
        return Math.min(itemsMap.size(), 5);
    }

    public List<K8sResourceNameContentConfig> getItems(String key) {
        return itemsMap.get(key);
    }

    public Set<K8sResourceType> getResourceTypes() {
        Set<K8sResourceType> set = new LinkedHashSet<>();
        itemsMap.values()
                .forEach(items -> items.forEach(e -> set.add(new K8sResourceType(e.getApiVersion(), e.getKind()))));
        return set;
    }

    public String getResourceTypesJson() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(getResourceTypes());
        return jsonArray.toString();
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        log.log(Level.INFO, "json:{0}", new Object[] {jo});

        JSONArray workloads = jo.getJSONArray("workloads");
        JSONObject workloadResourceMap = jo.getJSONObject("value");
        Map<String, List<K8sResourceNameContentConfig>> editResult = new LinkedHashMap<>();
        for (int i = 0; i < workloads.size(); i++) {
            String workload = workloads.getString(i);
            JSONObject resourceMap = workloadResourceMap.getJSONObject(workload);
            List<K8sResourceNameContentConfig> configs = new ArrayList<>(resourceMap.size());
            for (Iterator iter = resourceMap.keys(); iter.hasNext(); ) {
                Object resourceId = iter.next();
                JSONObject resource = resourceMap.getJSONObject(resourceId.toString());
                K8sResourceNameContentConfig config = new K8sResourceNameContentConfig(
                        resource.getString("apiVersion"), resource.getString("kind"), resource.getString("name"));
                config.setNamespace(resource.getOrDefault("namespace", "").toString());
                config.setContent(resource.getOrDefault("content", "").toString());
                configs.add(config);
            }
            editResult.put(workload, configs);
        }

        String name = jo.getString("name");

        return new ParameterValueImpl(name, editResult);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        try {
            JSONObject jo = req.getSubmittedForm();
            return createValue(req, jo);
        } catch (Exception e) {
            throw new IllegalStateException("Create value error.", e);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class ParameterValueImpl extends ParameterValue {

        private static final long serialVersionUID = -3871767739364710595L;

        private final Map<String, List<K8sResourceNameContentConfig>> result;

        public ParameterValueImpl(String name, Map<String, List<K8sResourceNameContentConfig>> result) {
            super(name);
            this.result = result;
        }

        @Override
        public Object getValue() {
            return result;
        }
    }

    @Extension
    @Symbol("kubeResourceAlter")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(KubeResourceAlterParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Kubernetes Resource Alter Parameter";
        }
    }
}

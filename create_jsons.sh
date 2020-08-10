 unzip newpets_1.json.zip
 mv WuoevMgF_newpets_1.json newpets_1.json
 tail -1 newpets_1.json
 tail -1 newpets_1.json > nplast.json
 cp newpets_1.json newpets_1.json.tmp ; sed '$ d' newpets_1.json.tmp > newpets_1.json
 rm newpets_1.json.tmp
 echo "{ \"pets\": [" > np.json
 cat newpets_1.json | awk '{print $0 ","}' >> np.json
 cat nplast.json >> np.json
 echo "]," >> np.json

 unzip orgs_1.json.zip
 mv WuoevMgF_orgs_1.json orgs_1.json
 tail -1 orgs_1.json
 tail -1 orgs_1.json > orlast.json
 cp orgs_1.json orgs_1.json.tmp ; sed '$ d' orgs_1.json.tmp > orgs_1.json
 rm orgs_1.json.tmp
 echo " \"orgs\": [" >> np.json
 cat orgs_1.json | awk '{print $0 ","}' >> np.json
 cat orlast.json >> np.json
 echo "]}" >> np.json

<?php
include_once 'connexion.php';
include_once 'GlobalAuditClass.php';
include_once 'AuditMetadatasDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'AuditNode.php';
include_once 'config.php';

/**
 * Objet DAO listant les differentes requetes disponibles sur les donnees d'audit
 * @author xqcg635
 *
 */
class AuditDAO {

	const REQUEST_GET_DATA_AND_METADATAS = "SELECT /*+ PARALLEL(16) */ DISTINCT ea.AUDIT_ID, ea.PROJECT, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP,mds.METADATA_KEY as KEY, mds.METADATA_VALUE AS VALUE FROM EAI_AUDIT ea ##SUB RQ CRITERIAS## LEFT JOIN (SELECT * FROM TBL_AUDIT_METADATA WHERE METADATA_KEY='INPUT_QUEUE')mds ON ea.AUDIT_ID = mds.AUDIT_ID WHERE 1 = 1  ##AUDIT CRITERIAS##";
	const REQUEST_GET_DATA_AND_METADATAS_GLOBAL = "SELECT /*+ PARALLEL(16) */ DISTINCT ea.AUDIT_ID, ea.PROJECT, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP,mds.METADATA_KEY as KEY, mds.METADATA_VALUE AS VALUE FROM VIEW_EAI_AUDIT_GLOBAL ea ##SUB RQ CRITERIAS## LEFT JOIN (SELECT * FROM TBL_AUDIT_METADATA WHERE METADATA_KEY='INPUT_QUEUE')mds ON ea.AUDIT_ID = mds.AUDIT_ID WHERE 1 = 1  ##AUDIT CRITERIAS##";
	
	const REQUEST_GET_DATA_ONLY = "SELECT /*+ PARALLEL(16) */ DISTINCT ea.AUDIT_ID, ea.PROJECT, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP, mds.METADATA_KEY as KEY, mds.METADATA_VALUE AS VALUE FROM EAI_AUDIT ea LEFT JOIN (SELECT * FROM TBL_AUDIT_METADATA WHERE METADATA_KEY='INPUT_QUEUE')mds ON ea.AUDIT_ID = mds.AUDIT_ID WHERE 1 = 1  ##AUDIT CRITERIAS##";
	const REQUEST_GET_DATA_ONLY_GLOBAL = "SELECT /*+ PARALLEL(16) */ DISTINCT ea.AUDIT_ID, ea.PROJECT, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP,mds.METADATA_KEY as KEY, mds.METADATA_VALUE AS VALUE FROM VIEW_EAI_AUDIT_GLOBAL ea LEFT JOIN (SELECT * FROM TBL_AUDIT_METADATA WHERE METADATA_KEY='INPUT_QUEUE')mds ON ea.AUDIT_ID = mds.AUDIT_ID WHERE 1 = 1  ##AUDIT CRITERIAS##";

	const REQUEST_GET_FULL_DATA_AND_METADATAS = "SELECT /*+ PARALLEL(16) */ ea.AUDIT_ID, ea.PROJECT, ea.DETAILS, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP FROM EAI_AUDIT ea ##SUB RQ CRITERIAS## WHERE 1 = 1 ##AUDIT CRITERIAS##";
	const REQUEST_GET_FULL_DATA_AND_METADATAS_GLOBAL = "SELECT /*+ PARALLEL(16) */ ea.AUDIT_ID, ea.PROJECT, ea.DETAILS, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP , mds.METADATA_KEY as KEY, mds.METADATA_VALUE AS VALUE FROM VIEW_EAI_AUDIT_GLOBAL ea ##SUB RQ CRITERIAS## WHERE 1 = 1 ##AUDIT CRITERIAS##";
	
	const REQUEST_GET_FULL_DATA_ONLY = "SELECT /*+ PARALLEL(16) */ ea.AUDIT_ID, ea.PROJECT, ea.DETAILS, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP FROM EAI_AUDIT ea WHERE 1 = 1 ##AUDIT CRITERIAS##";
	const REQUEST_GET_FULL_DATA_ONLY_GLOBAL = "SELECT /*+ PARALLEL(16) */ ea.AUDIT_ID, ea.PROJECT, ea.DETAILS, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP FROM VIEW_EAI_AUDIT_GLOBAL ea WHERE 1 = 1 ##AUDIT CRITERIAS##";
	
	const REQUEST_COUNT_DATA_AND_METADATAS = "select /*+ PARALLEL(16) */ COUNT(DISTINCT EA.AUDIT_ID) AS NB FROM EAI_AUDIT ea ##SUB RQ CRITERIAS## WHERE 1 = 1 ##AUDIT CRITERIAS##";
	const REQUEST_COUNT_DATA_AND_METADATAS_GLOBAL = "select /*+ PARALLEL(16) */ COUNT(DISTINCT EA.AUDIT_ID) AS NB FROM VIEW_EAI_AUDIT_GLOBAL ea ##SUB RQ CRITERIAS## WHERE 1 = 1 ##AUDIT CRITERIAS##";
	
	const REQUEST_COUNT_DATA_ONLY = "select /*+ PARALLEL(16) */ COUNT(DISTINCT EA.AUDIT_ID) AS NB FROM EAI_AUDIT ea WHERE 1 = 1 ##AUDIT CRITERIAS##";
	const REQUEST_COUNT_DATA_ONLY_GLOBAL = "select /*+ PARALLEL(16) */ COUNT(DISTINCT EA.AUDIT_ID) AS NB FROM VIEW_EAI_AUDIT_GLOBAL ea WHERE 1 = 1 ##AUDIT CRITERIAS##";
	
	const REQUEST_GET_AUDIT_LINE_GLOBAL = "SELECT /*+ PARALLEL(16) */ ea.AUDIT_ID, ea.PROJECT, ea.CODE, ea.CODIFIER, ea.STATUT, ea.DATA, ea.DETAILS , TO_CHAR(ea.INSERT_TIMESTAMP,'DD/MM/YYYY HH24:MI:SS') AS INSERT_TIMESTAMP, LENGTH(DETAILS) AS DETAILS_LENGTH FROM VIEW_EAI_AUDIT_GLOBAL ea WHERE ea.AUDIT_ID = :audit_id";

	/**
	 * BDD Connection
	 */	
	private $connector;
	
	/**
	 * Default constructor
	 */
	public function __construct(){
		$this->connector = new connecter();
	}

	/**
	 * Format bind variables
	 */
	private function formatRequestCondition($pValue, $pBindVariableName) {
		if (stripos($pValue, '%', 0) !== false) {
			$result = ' like '.$pBindVariableName;
		} else {
			$result = ' = '.$pBindVariableName;
		}
	
		return $result;
	}
	
	/**
	 * Format bind variables
	 */
	private function formatRequestConditionForDetails($pValue, $pBindVariableName) {
		if (stripos($pValue, '%', 0) !== false) {
			$result = " clob_like(ea.DETAILS,".$pBindVariableName.")='Y'";
		} else {
			$result = ' = '.$pBindVariableName;
		}
	
		return $result;
	}

	/**
	 * Gets the list of EAI projects
	 */
	public function getProjetNames(){
		/* $query = "select unique project as project from eai_audit where project is not null order by project asc";
		$connect = $this->connector->connection();
		$select = array();
		if ($connect){
			$stmt = oci_parse($connect, $query);
			oci_execute($stmt, OCI_DEFAULT);
			$i =0;
			while ($row =oci_fetch_array($stmt, OCI_RETURN_NULLS)){
				$select[$i] = $row['PROJECT'];
				$i++;
			}
		}
		$this->connector->deconnection();
		return $select; */
		$projectList = file(realpath("./config/auditProjectList.txt"));
		foreach ($projectList as &$projectName) {
			$projectName = trim($projectName);
		}
		return $projectList;
	}

	
	
	/**
	 * Build TBL_AUDIT_METADTA SQL Criteria clause
	 */
	private function buildMDCriteriaClause($criteres) {
		$subRequest = '';
		
		if (is_array($criteres['audit_metadata'])) {
			// Plusieurs elements					
			for ($i=0; $i < sizeof($criteres['audit_metadata']) ; $i++) {
				$key = array_shift(array_keys($criteres['audit_metadata'][$i]));
				$value = $criteres['audit_metadata'][$i][$key];
				
				if (stripos($value, '%')!== false) {
					$request .= " and (am.METADATA_KEY = :".$key.$i." AND  am.METADATA_VALUE like :".$key."_VALUE$i)";
				} else {
					$request .= " and (am.METADATA_KEY = :".$key.$i." AND  am.METADATA_VALUE = :".$key."_VALUE$i)";
				}
											
				$subRequest .= $request;
			}
		} else {
			// 1 seul element
			$key = array_shift(array_keys($criteres['audit_metadata']));
			$value = $criteres['audit_metadata'][$key];

			if (stripos($value, '%')!== false) {
				$subRequest .= " and (am.METADATA_KEY = :".$key." AND  am.METADATA_VALUE like :".$key."_VALUE)";
			} else {
				$subRequest .= " and (am.METADATA_KEY = :".$key." AND  am.METADATA_VALUE = :".$key."_VALUE)";
			}
		}
	
		return $subRequest;
	}	

	/**
	 * Build EAI_FLOW_METADATAS SQL Criteria clause
	 */
	private function buildAuditV2MDCriteriaClause($criteres) {
		$subRequest = '';
	
		if (is_array($criteres['audit_metadata'])) {
			// Plusieurs elements
			for ($i=0; $i < sizeof($criteres['audit_metadata']) ; $i++) {
				$key = array_shift(array_keys($criteres['audit_metadata'][$i]));
				$value = $criteres['audit_metadata'][$i][$key];
	
				if (stripos($value, '%')!== false) {
					$request .= " and (FMD.PARAM_NAME = :".$key.$i." AND FMD.PARAM_VALUE like :".$key."_VALUE$i)";
				} else {
					$request .= " and (FMD.PARAM_NAME = :".$key.$i." AND FMD.PARAM_VALUE = :".$key."_VALUE$i)";
				}
					
				$subRequest .= $request;
			}
		} else {
			// 1 seul element
			$key = array_shift(array_keys($criteres['audit_metadata']));
			$value = $criteres['audit_metadata'][$key];
	
			if (stripos($value, '%')!== false) {
				$subRequest .= " and (FMD.PARAM_NAME = :".$key." AND FMD.PARAM_VALUE like :".$key."_VALUE)";
			} else {
				$subRequest .= " and (FMD.PARAM_NAME = :".$key." AND MD.PARAM_VALUE = :".$key."_VALUE)";
			}
		}
	
		return $subRequest;
	}
	
   /**
	* Build EAI_AUDIT SQL Criteria clause
	*/
	private function buildAuditCriteriaClause($criteres) {
		$critere_sql = "";
		if(((isset($criteres['CODE'])) && (!empty($criteres['CODE']))) || $criteres['CODE']=='0'){
			$critere_sql .= ' and ea.CODE'.$this->formatRequestCondition($criteres['CODE'], ':criteres_CODE');
		}
		if(((isset($criteres['PROJECT'])) && (!empty($criteres['PROJECT'])))|| $criteres['PROJECT']=='0'){
			$critere_sql .= ' and ea.PROJECT'.$this->formatRequestCondition($criteres['PROJECT'], ':criteres_PROJECT');
		}
		if(((isset($criteres['FLOWS'])) && (!empty($criteres['FLOWS'])))|| $criteres['FLOWS']=='0'){
			$flows = explode(",", $criteres['FLOWS']);
			$i=0;
			foreach($flows as $flow){
				if($i==0){
					$critere_sql .= " and (ea.PROJECT LIKE '".$flow."'";
				}
				else {
					$critere_sql .= " or ea.PROJECT LIKE '".$flow."'";
				}
			$i++;
			}
			$critere_sql .=")";
		}
		if(((isset($criteres['CODIFIER'])) && (!empty($criteres['CODIFIER'])))|| $criteres['CODIFIER']=='0'){
			$critere_sql .= ' and ea.CODIFIER'.$this->formatRequestCondition($criteres['CODIFIER'], ':criteres_CODIFIER');
		}
		if(((isset($criteres['DATA'])) && (!empty($criteres['DATA'])))|| $criteres['DATA']=='0'){
			$critere_sql .= ' and ea.DATA'.$this->formatRequestCondition($criteres['DATA'], ':criteres_DATA');
		}
		if(((isset($criteres['TIMESTAMP'])) && (!empty($criteres['TIMESTAMP'])))|| $criteres['TIMESTAMP']=='0'){
			$critere_sql .= " and ea.INSERT_TIMESTAMP >= TO_DATE(:criteres_TIMESTAMP,'DD/MM/YYYY HH24:MI')";
		}
		if(((isset($criteres['TIMESTAMP2'])) && (!empty($criteres['TIMESTAMP2'])))|| $criteres['TIMESTAMP2']=='0'){
			$critere_sql .= " and ea.INSERT_TIMESTAMP <= TO_DATE(:criteres_TIMESTAMP2,'DD/MM/YYYY HH24:MI')";
		}
		if(((isset($criteres['STATUS'])) && (!empty($criteres['STATUS'])))|| $criteres['STATUS']=='0'){
			if($criteres['STATUS'] == "Error") {
				$critere_sql .= " and ( ea.STATUT IN ('Error','4')) ";
			} else if($criteres['STATUS'] == "Success") {
				$critere_sql .= " and ( ea.STATUT IN ('Success','7','8') ) ";
			} else if($criteres['STATUS'] == "Warning") {
				$critere_sql .= " and ( ea.STATUT = 'Warning') ";
			} else {
				$critere_sql .= " and ea.STATUT = :criteres_STATUS";
			}
		}
		if(((isset($criteres['DETAILS'])) && (!empty($criteres['DETAILS'])))|| $criteres['DETAILS']=='0'){
			$critere_sql .= ' and ea.DETAILS'.$this->formatRequestCondition($criteres['DETAILS'], ':criteres_DETAILS');
		}
		return $critere_sql;
	}
	
	/**
	 * Build EAI_FLOW_AUDIT SQL Criteria clause
	 */
	private function buildAuditV2CriteriaClause($criteres,$useAuditArchive) {
		$critere_sql = "";

		if (array_key_exists('TECHNICAL_ID', $criteres)) {
			if ((isset($criteres['TECHNICAL_ID'])) && (!empty($criteres['TECHNICAL_ID']))) {
				$critere_sql .= ' and FA.TECHNICAL_ID'.$this->formatRequestCondition($criteres['TECHNICAL_ID'], ':criteres_TECHNICAL_ID');
			}				
		}
		
		if (array_key_exists('PARENT_ID', $criteres)) {
			if ((isset($criteres['PARENT_ID'])) && (!empty($criteres['PARENT_ID']))) {
				$critere_sql .= ' and FA.PARENT_ID'.$this->formatRequestCondition($criteres['PARENT_ID'], ':criteres_PARENT_ID');
			}
		}
		
		if (array_key_exists('PROCESSING_LEVEL', $criteres)) {
			if ((isset($criteres['PROCESSING_LEVEL'])) && (!empty($criteres['PROCESSING_LEVEL']))) {
				$critere_sql .= ' and FA.PROCESSING_LEVEL'.$this->formatRequestCondition($criteres['PROCESSING_LEVEL'], ':criteres_PROCESSING_LEVEL');
			}
		}
		
		if (array_key_exists('PROCESSING_ID', $criteres)) {
			if ((isset($criteres['PROCESSING_ID'])) && (!empty($criteres['PROCESSING_ID']))) {
				$critere_sql .= ' and FA.PROCESSING_ID'.$this->formatRequestCondition($criteres['PROCESSING_ID'], ':criteres_PROCESSING_ID');
			}
		}
		
		if (array_key_exists('PROCESSING_DATE_LOW', $criteres)) {
			if ((isset($criteres['PROCESSING_DATE_LOW'])) && (!empty($criteres['PROCESSING_DATE_LOW']))) {
				$critere_sql .= " and FA.PROCESSING_DATE >= TO_DATE(:criteres_PROCESSING_DATE_LOW,'DD-MM-YYYY HH24:MI')";
			}
		}
		
		if (array_key_exists('PROCESSING_DATE_HIGH', $criteres)) {
			if ((isset($criteres['PROCESSING_DATE_HIGH'])) && (!empty($criteres['PROCESSING_DATE_HIGH']))) {
				$critere_sql .= " and FA.PROCESSING_DATE <= TO_DATE(:criteres_PROCESSING_DATE_HIGH,'DD-MM-YYYY HH24:MI')";
			}
		}
		
		if (array_key_exists('INSERT_DATE_LOW', $criteres)) {
			if ((isset($criteres['INSERT_DATE_LOW'])) && (!empty($criteres['INSERT_DATE_LOW']))) {
				$critere_sql .= " and FA.INSERT_DATE >= TO_DATE(:criteres_INSERT_DATE_LOW,'DD-MM-YYYY HH24:MI')";
			}
		}
		
		if (array_key_exists('INSERT_DATE_HIGH', $criteres)) {
			if ((isset($criteres['INSERT_DATE_HIGH'])) && (!empty($criteres['INSERT_DATE_HIGH']))) {
				$critere_sql .= " and FA.INSERT_DATE <= TO_DATE(:criteres_INSERT_DATE_HIGH,'DD-MM-YYYY HH24:MI')";
			}
		}
		
		if (array_key_exists('EAI_PROJECT', $criteres)) {
			if ((isset($criteres['EAI_PROJECT'])) && (!empty($criteres['EAI_PROJECT']))) {
				$critere_sql .= ' and FA.EAI_PROJECT'.$this->formatRequestCondition($criteres['EAI_PROJECT'], ':criteres_EAI_PROJECT');
			}
		}
		
		if (array_key_exists('LABEL', $criteres)) {
			if ((isset($criteres['LABEL'])) && (!empty($criteres['LABEL']))) {
				$critere_sql .= ' and FA.LABEL'.$this->formatRequestCondition($criteres['LABEL'], ':criteres_LABEL');
			}
		}
	
		if (array_key_exists('MASTER_REF', $criteres)) {
			if ((isset($criteres['MASTER_REF'])) && (!empty($criteres['MASTER_REF']))) {
				if ($useAuditArchive){
				$critere_sql .= ' AND FA.TECHNICAL_ID IN (SELECT TECHNICAL_ID FROM VIEW_EAI_FLOW_METADATAS_GLOBAL WHERE PARAM_NAME = \'MESSAGEID\' AND PARAM_VALUE'.$this->formatRequestCondition($criteres['MASTER_REF'], ':criteres_MASTER_REF' ).')';					
				}
				else {
					$critere_sql .= ' AND FA.TECHNICAL_ID IN (SELECT TECHNICAL_ID FROM EAI_FLOW_METADATAS WHERE PARAM_NAME = \'MESSAGEID\' AND PARAM_VALUE'.$this->formatRequestCondition($criteres['MASTER_REF'], ':criteres_MASTER_REF' ).')';
				}
			}
		}
		
		if (array_key_exists('EXT_REF', $criteres)) {
			if ((isset($criteres['EXT_REF'])) && (!empty($criteres['EXT_REF']))) {
				if ($useAuditArchive){
				$critere_sql .= ' AND FA.TECHNICAL_ID IN (SELECT TECHNICAL_ID FROM VIEW_EAI_FLOW_METADATAS_GLOBAL WHERE PARAM_NAME IN (\'EXT_ID\',\'DATE\') AND PARAM_VALUE'.$this->formatRequestCondition($criteres['EXT_REF'], ':criteres_EXT_REF' ).')';
				}
				else {
					$critere_sql .= ' AND FA.TECHNICAL_ID IN (SELECT TECHNICAL_ID FROM EAI_FLOW_METADATAS WHERE PARAM_NAME IN (\'EXT_ID\',\'DATE\') AND PARAM_VALUE'.$this->formatRequestCondition($criteres['EXT_REF'], ':criteres_EXT_REF' ).')';
				}
			}
		}
		
		if (array_key_exists('STATUS', $criteres)) {
			if(((isset($criteres['STATUS'])) && (!empty($criteres['STATUS'])))|| $criteres['STATUS']=='0'){
				if($criteres['STATUS'] == "Error") {
					$critere_sql .= " and ( FA.STATUS IN ('Error','4')) ";
				} else if($criteres['STATUS'] == "Success") {
					$critere_sql .= " and ( FA.STATUS IN ('Success','7','8') ) ";
				} else if($criteres['STATUS'] == "Warning") {
					$critere_sql .= " and ( FA.STATUS = 'Warning') ";
				} else {
					$critere_sql .= " and FA.STATUS = :criteres_STATUS";
				}
			}
		}
	
		return $critere_sql;
	}
	
	/**
	 * Build EAI_AUDIT SQL Criteria clause
	 */
	private function builMDSubRequest($criteres,$useAuditArchive) {
		$subRequest = '';
		if (is_array($criteres['audit_metadata'])) {
			// Plusieurs elements					
			for ($i=0; $i < sizeof($criteres['audit_metadata']) ; $i++) {
				$temp = array_keys($criteres['audit_metadata'][$i]);
				$key = array_shift($temp);
				$value = $criteres['audit_metadata'][$i][$key];
				if ($useAuditArchive){
				$request = "INNER JOIN (SELECT am".$i.".audit_id FROM VIEW_AUDIT_METADATA_GLOBAL am".$i." WHERE 1=1 ";
				}
		//		else {
					$request = "INNER JOIN (SELECT am".$i.".audit_id FROM TBL_AUDIT_METADATA am".$i." WHERE 1=1 ";
		//		}
				
				if (stripos($value, '%')!== false) {
					$request .= " and (am".$i.".METADATA_KEY = '".$key."' AND  am".$i.".METADATA_VALUE like '".$value."')";
				} else {
					$request .= " and (am".$i.".METADATA_KEY = '".$key."' AND  am".$i.".METADATA_VALUE = '".$value."')";
				}
								
				$request .= ") RQ".$i." ON RQ".$i.".AUDIT_ID = EA.AUDIT_ID ";
				
				$subRequest .= $request;
			}
		} else {
			// 1 seul element
			$key = array_shift(array_keys($criteres['audit_metadata']));
			$value = $criteres['audit_metadata'][$key];
			if ($useAuditArchive){
			$request = $request = "INNER JOIN (SELECT am0.audit_id FROM VIEW_AUDIT_METADATA_GLOBAL am0 WHERE 1=1 ";				
			}
			//else {
			//	$request = $request = "INNER JOIN (SELECT am0.audit_id FROM TBL_AUDIT_METADATA am0 WHERE 1=1 ";
			//}
			
			if (stripos($value, '%')!== false) {
				$subRequest .= " and (am0.METADATA_KEY = '".$key."' AND am0.METADATA_VALUE like '".$value."')";
			} else {
				$subRequest .= " and (am0.METADATA_KEY = '".$key."' AND am0.METADATA_VALUE = '".$value."')";
			}

			$request .= ") RQ0 ON RQ0.AUDIT_ID = EA.AUDIT_ID ";
		}
				
		return $subRequest;
	}
	
	/**
	 * Build EAI_AUDIT SQL Criteria clause
	 */
	private function builAuditV2MDSubRequest($criteres) {
		$subRequest = '';
		if (is_array($criteres['audit_metadata'])) {
			// Plusieurs elements
			for ($i=0; $i < sizeof($criteres['audit_metadata']) ; $i++) {
				$temp = array_keys($criteres['audit_metadata'][$i]);
				$key = array_shift($temp);
				$value = $criteres['audit_metadata'][$i][$key];
	
				$request = "INNER JOIN (SELECT FMD".$i.".TECHNICAL_ID FROM EAI_FLOW_METADATAS FMD".$i." WHERE 1=1 ";
	
				if (stripos($value, '%')!== false) {
					$request .= " and (FMD".$i.".PARAM_NAME = '".$key."' AND FMD".$i.".PARAM_VALUE like '".$value."')";
				} else {
					$request .= " and (FMD".$i.".PARAM_NAME = '".$key."' AND FMD".$i.".PARAM_VALUE = '".$value."')";
				}
	
				$request .= ") RQ".$i." ON RQ".$i.".TECHNICAL_ID = FA.TECHNICAL_ID ";
	
				$subRequest .= $request;
			}
		} else {
			// 1 seul element
			$key = array_shift(array_keys($criteres['audit_metadata']));
			$value = $criteres['audit_metadata'][$key];
	
			$request = $request = "INNER JOIN (SELECT FMD0.TECHNICAL_ID FROM EAI_FLOW_METADATAS FMD0 WHERE 1=1 ";
				
			if (stripos($value, '%')!== false) {
				$subRequest .= " and (FMD0.PARAM_NAME = '".$key."' AND FMD0.PARAM_VALUE like '".$value."')";
			} else {
				$subRequest .= " and (FMD0.PARAM_NAME = '".$key."' AND FMD0.PARAM_VALUE = '".$value."')";
			}
	
			$request .= ") RQ0 ON RQ0.TECHNICAL_ID = FA.TECHNICAL_ID ";
		}
	
		return $subRequest;
	}
	
	/**
	 * Gets the list of EAI status
	 */
	public function getStatus(){
		$query = "select /*+ PARALLEL(16) */ unique statut from eai_audit where statut is not null order by statut asc";
		$connect = $this->connector->connection();
		$select = array();
		if ($connect){
			$stmt = oci_parse($connect, $query);
			oci_execute($stmt,OCI_DEFAULT);
			$i =0;
			while ($row =oci_fetch_array($stmt, OCI_RETURN_NULLS)){
				$select[$i] = $row['STATUT'];
				$i++;
			}
		}
		$this->connector->deconnection();
		return $select ;
	}

	
	/**
	 * Get audit line 
	 * $auditId    : Audit Line Id
	 */
	public function getAuditLineDetails($pAuditId){
		try {
		$connect = $this->connector->connection();
		$result = null;
		if ($connect){
			// Preparing statement
			$stmt = oci_parse($connect, self::REQUEST_GET_AUDIT_LINE_GLOBAL);
					
			// Setting up the binding variable
			oci_bind_by_name($stmt, ':audit_id', $pAuditId);
				
			// Execution
			oci_execute($stmt, OCI_DEFAULT);

			// Parsing the results
			$row = oci_fetch_array($stmt, OCI_RETURN_NULLS);
			$detailsLob=$row['DETAILS'];
  
			$req_dump = print_r($row, TRUE);
		     $fp = fopen("/serveur_apps/tmp/AuditDAO_row.txt", 'w');
			fwrite($fp, $req_dump);
			fclose($fp);


   if ($detailsLob == NULL)
   { 
   $audit = new Audit($row['CODE'], $row['CODIFIER'], '' , $row['PROJECT'], utf8_encode($row['DATA']), $row['INSERT_TIMESTAMP'], $row['STATUT'], $row['AUDIT_ID']);
   $audit->detailsLength = 0;
   }
   else
   {
   $details = '';
   $chunkSize = 65534;
	while (!$detailsLob->eof()) {
    $details .= $detailsLob->read($chunkSize);
	}
   $audit = new Audit($row['CODE'], $row['CODIFIER'], utf8_encode($details), $row['PROJECT'], utf8_encode($row['DATA']), $row['INSERT_TIMESTAMP'], $row['STATUT'], $row['AUDIT_ID'], $row['DETAILS_LENGTH']);
   $audit->detailsLength = $row['DETAILS_LENGTH'];
   }
			// Metadatas ?
			$auditMetadatasDao = new AuditMetadataDAO();
			$audit->metadatas=$auditMetadatasDao->getAuditMetadatas($pAuditId);				
			$result = $audit;
			$this->connector->deconnection();
		}
		return $result;
		} catch(Exception $e){
			echo "Erreur " . $e->getMessage();
		}
	}
	/**
	 * Returns audit datas from db matching the given criterias
	 * $pagination    : Current page
	 * $intervalle    : Number of lines per page
	 * $sortCriteria  : Sort criteria
	 * $criteres      : Search criterias
	 * $sensAff		  : Search order
	 */
	public function getAuditData($pagination, $interval, $sortCriteria, $criteres, $sensAff,$useAuditArchive){
		$req_dump = print_r($criteres, TRUE);
		     $fp = fopen("/serveur_apps/tmp/AuditDAO_getAuditData.txt", 'w');
			fwrite($fp, $req_dump);
			fclose($fp);
		$connect = $this->connector->connection();
		$auditResult = null;
		if ($connect){
			$QUERY = null;
			$QUERY_CNT = null;
							
			// Selecting SQL Request
			if ((isset($criteres['audit_metadata'])) && (!empty($criteres['audit_metadata']))) {
				// Some additionnal metatda
				if ($useAuditArchive){
					//$QUERY = self::REQUEST_GET_DATA_AND_METADATAS_GLOBAL;
					$QUERY = self::REQUEST_GET_DATA_AND_METADATAS;
					$QUERY_CNT = self::REQUEST_COUNT_DATA_AND_METADATAS_GLOBAL;
				}
				else {
					
					$QUERY = self::REQUEST_GET_DATA_AND_METADATAS;
					$QUERY_CNT = self::REQUEST_COUNT_DATA_AND_METADATAS;
				}
			} else {
				// No metadata
				if ($useAuditArchive){
					$QUERY = self::REQUEST_GET_DATA_ONLY_GLOBAL;
					$QUERY_CNT = self::REQUEST_COUNT_DATA_ONLY_GLOBAL;
				}
				else {
					$QUERY = self::REQUEST_GET_DATA_ONLY;
					$QUERY_CNT = self::REQUEST_COUNT_DATA_ONLY;
				}
			}
			
			// Search criterias
			$auditClause = $this->buildAuditCriteriaClause($criteres);
			
			$QUERY = str_replace('##AUDIT CRITERIAS##', $auditClause, $QUERY);
			$QUERY_CNT= str_replace('##AUDIT CRITERIAS##', $auditClause, $QUERY_CNT);
			
			// Functionnal Metadata
			if ((isset($criteres['audit_metadata'])) && (!empty($criteres['audit_metadata']))) {
				
				$subRequest = $this->builMDSubRequest($criteres,$useAuditArchive);
				$QUERY = str_replace('##SUB RQ CRITERIAS##', $subRequest, $QUERY);
				$QUERY_CNT = str_replace('##SUB RQ CRITERIAS##', $subRequest, $QUERY_CNT);
			}
			// Sort order
			if((isset($sortCriteria)) && (!empty($sortCriteria))) {
				switch ($sortCriteria) {
					case 'CODE' :
						$QUERY .= " order by ea.CODE ".$sensAff;
						break;
					case 'CODIFIER' :
						$QUERY .= " order by ea.CODIFIER ".$sensAff;
						break;
					case 'PROJECT' :
						$QUERY .= " order by ea.PROJECT ".$sensAff;
						break;
					case 'DATA' :
						$QUERY .= " order by ea.DATA ".$sensAff;
						break;
					case 'TIMESTAMP' :
						$QUERY .= " order by INSERT_TIMESTAMP ".$sensAff;
						break;
					case 'STATUT' :
						$QUERY .= " order by ea.STATUT ".$sensAff;
						break;
					default:
						$QUERY .= " order by ea.AUDIT_ID ".$sensAff;
				}
			} else {
				// Default sort
				$QUERY .= " order by ea.AUDIT_ID DESC";
			}
			// Preparing statement
			$stmtc = oci_parse($connect, $QUERY_CNT);
			// SQL Variables to bind
			$criteria_labels = array("CODE", "PROJECT", "CODIFIER", "DATA", "TIMESTAMP", "TIMESTAMP2", "STATUS","DETAILS");
			for ($i=0;$i<sizeof($criteria_labels);$i++) {
				if ((isset($criteres[$criteria_labels[$i]])) && (!empty($criteres[$criteria_labels[$i]]))) {
					oci_bind_by_name($stmtc, ':criteres_'.$criteria_labels[$i], iconv('UTF-8', 'ISO-8859-15', $criteres[$criteria_labels[$i]] ), -1, SQLT_CHR);						
				}
			}
			
			if($pagination==0){
			// Executing query
			$cmdResult = oci_execute($stmtc, OCI_NO_AUTO_COMMIT);
			// Statement is freed if any errors occurs
			if (!$cmdResult) {
				$e = oci_error($stmtc);
				throw new Exception($e['message']);
			}
			$ligne = oci_fetch_array($stmtc, OCI_RETURN_NULLS);
			$total = $ligne['NB'];
			$_SESSION['TOTAOL_AUDIT_NB_LINE'] = $total;
			oci_free_statement($stmtc);
			}
			else {
			 $total = $_SESSION['TOTAOL_AUDIT_NB_LINE'];
			}
			
		    $QUERY = "SELECT /*+ PARALLEL(16) */ * FROM (".$QUERY.") OFFSET :interval1 ROWS FETCH NEXT :interval2 ROWS ONLY";
			$stmt = oci_parse($connect, $QUERY);
			
			
			
			for ($i=0;$i<sizeof($criteria_labels);$i++) {
				if ((isset($criteres[$criteria_labels[$i]])) && (!empty($criteres[$criteria_labels[$i]]))) {
					oci_bind_by_name($stmt, ':criteres_'.$criteria_labels[$i], iconv('UTF-8', 'ISO-8859-15', $criteres[$criteria_labels[$i]] ), -1, SQLT_CHR);						
				}
			}
			//$interval1 = ($pagination * $interval);
			$interval1 = $pagination;
			//$interval2 = (($pagination + 1) * $interval);
			$interval2 = $interval;

			oci_bind_by_name($stmt, ':interval1', $interval1);
			oci_bind_by_name($stmt, ':interval2', $interval2);
			$cmdResult = oci_execute($stmt, OCI_NO_AUTO_COMMIT);
			
			// Statement is freed if any errors occurs
			if (!$cmdResult) {
				$e = oci_error($stmt);
				throw new Exception($e['message']);
			}
			// Fetching results
			$auditResult = new GlobalAudit($total);
			while ($row =oci_fetch_array($stmt, OCI_RETURN_NULLS)){
				$audit = new Audit($row['CODE'], $row['CODIFIER'], $row['PROJECT'], utf8_encode($row['DATA']), $row['INSERT_TIMESTAMP'], $row['STATUT'], $row['AUDIT_ID']);
				if($row['KEY']=="INPUT_QUEUE"){
				$audit->inputQueue=$row['VALUE'];	
				}
				$auditResult->addAuditLine($audit);
			}
			$this->connector->deconnection();
		}
		return $auditResult;
	}

	
	/**
	 * Returns the label of keyword
	 */
	private function getKeywordlabel($keywords, $key) {
		foreach ($keywords as $keyword) {
			if ($keyword->key === $key) {
				return $keyword->label;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns audit datas from db matching the given criterias for export purposes
	 * $sortCriteria  : Sort criteria
	 * $criteres      : Search criterias
	 * $sensAff		  : Search order
	 */
	public function getFullAuditData($sortCriteria, $criteres, $sensAff,$useAuditArchive){
		
		$connect = $this->connector->connectionUTF8();
		$resultLines = Array();
		$keywords = Array();
		if ($connect){

			// Type Export : All pages or current page
			if ((isset($criteres['typeExport'])) && (!empty($criteres['typeExport']))) {
				// Some additionnal metatda
				$typeExport = $criteres['typeExport'];
			} else {
				// Default => Full
				$typeExport = 'all';
			}

			// Current page 
			$currentPage = 0 ;
			if ((isset($criteres['pagination'])) && (!empty($criteres['pagination']))) {
				// Some additionnal metatda
				$currentPage = $criteres['pagination'];
			}

			$QUERY = null;								
			// Selecting SQL Request
			if ((isset($criteres['audit_metadata'])) && (!empty($criteres['audit_metadata']))) {
				if ($useAuditArchive){
					//Some additionnal metatda
					$QUERY = self::REQUEST_GET_FULL_DATA_AND_METADATAS_GLOBAL;
				}
				else{
					//Some additionnal metatda
					$QUERY = self::REQUEST_GET_FULL_DATA_AND_METADATAS;
				}

			} else {
				// No metadata
				if ($useAuditArchive){
					$QUERY = self::REQUEST_GET_FULL_DATA_ONLY_GLOBAL;
				}
				else {					
					$QUERY = self::REQUEST_GET_FULL_DATA_ONLY;
				}
			}

			// Search criterias
			$auditClause = $this->buildAuditCriteriaClause($criteres);
			
			$QUERY = str_replace('##AUDIT CRITERIAS##', $auditClause, $QUERY);
				
			// Functionnal Metadata
			if ((isset($criteres['audit_metadata'])) && (!empty($criteres['audit_metadata']))) {
				$subRequest = $this->builMDSubRequest($criteres,$useAuditArchive);
				$QUERY = str_replace('##SUB RQ CRITERIAS##', $subRequest, $QUERY);
			}

			// Sort order
			if((isset($sortCriteria)) && (!empty($sortCriteria))) {
				switch ($sortCriteria) {
					case 'CODE' :
						$QUERY .= " order by ea.CODE ".$sensAff;
						break;
					case 'CODIFIER' :
						$QUERY .= " order by ea.CODIFIER ".$sensAff;
						break;
					case 'PROJECT' :
						$QUERY .= " order by ea.PROJECT ".$sensAff;
						break;
					case 'DATA' :
						$QUERY .= " order by ea.DATA ".$sensAff;
						break;
					case 'TIMESTAMP' :
						$QUERY .= " order by ea.INSERT_TIMESTAMP ".$sensAff;
						break;
					case 'STATUT' :
						$QUERY .= " order by ea.STATUT ".$sensAff;
						break;
					default:
						$QUERY .= " order by ea.AUDIT_ID ".$sensAff;
				}
			} else {
				// Default sort
				$QUERY .= " order by ea.AUDIT_ID DESC";
			}
			
			if ($typeExport === "current") {
				// Selection of current page
				$QUERY = "SELECT /*+ PARALLEL(16) */ * FROM (SELECT * FROM (SELECT RQ.*, rownum as cnt FROM (".$QUERY.") RQ ) WHERE CNT <= :interval2) WHERE CNT >= :interval1";				
			}			
			$stmt = oci_parse($connect, $QUERY);
			if (!$stmt) {
				$e = oci_error($connect);
				throw new Exception($e['message']);
			}
			
			
			// Criteres simples
			$criteria_labels = array("CODE", "PROJECT", "CODIFIER", "DATA", "TIMESTAMP", "TIMESTAMP2", "STATUS","DETAILS");
			for ($i=0;$i<sizeof($criteria_labels);$i++) {
				if ((isset($criteres[$criteria_labels[$i]])) && (!empty($criteres[$criteria_labels[$i]]))) {
					oci_bind_by_name($stmt, ':criteres_'.$criteria_labels[$i], $criteres[$criteria_labels[$i]]);
				}
			}	

			if ($typeExport === "current") {
				// Selection of current page
				$interval1 = ($currentPage * $criteres['intervalle'])+1;
				$interval2 = (($currentPage + 1) * $criteres['intervalle']);
							
				oci_bind_by_name($stmt, ':interval1', $interval1);
				oci_bind_by_name($stmt, ':interval2', $interval2);
			}
			
			$cmdResult = oci_execute($stmt, OCI_NO_AUTO_COMMIT);
			// Statement is freed if any errors occurs
			if (!$cmdResult) {
				$e = oci_error($stmt);
				throw new Exception($e['message']);
			}
			
			// Fetching results
			$auditResult = new GlobalAudit();
			//$metadataDao = new AuditMetadataDAO();
			while ($row =oci_fetch_array($stmt, OCI_RETURN_NULLS)){
				
				// Creating audit object
				$detailsLob = $row['DETAILS'];
				
				$detailsLobFull = NULL;
				if ((isset($detailsLob)) && (!empty($detailsLob) && ($detailsLob->size()>0))) {
					$detailsLobFull = $detailsLob->read($detailsLob->size());
				}
				
				$audit = new Audit($row['CODE'], $row['CODIFIER'], $detailsLobFull, $row['PROJECT'], $row['DATA'], $row['INSERT_TIMESTAMP'], $row['STATUT'], $row['AUDIT_ID']);

				// Metadatas
				if (!array_key_exists($audit->auditId, $resultLines)) {
					// Audit Obj needs to be added to result tab
					$resultLines[$audit->auditId] = $audit;
				}
				
				// Adding MD
				//$mds = $metadataDao->getAuditMetadatas($audit->auditId);
				//if (count($mds)>0) {
//					$resultLines[$audit->auditId]->metadatas = $mds;
	//			}
				$row = NULL;
			}
			// Searching for MD labels if any			
			$this->connector->deconnection();
		}
	
		return $resultLines;
	}
	
	public function getMessageDetails($messageId){
		$connect = $this->connector->connection();
		$result = null;
		$QUERY ="SELECT /*+ PARALLEL(16) */ DETAILS FROM EAI_AUDIT WHERE AUDIT_ID=".$messageId;
		if ($connect){
			// Preparing statement
			$stmt = oci_parse($connect, $QUERY);
			// Execution
			oci_execute($stmt, OCI_DEFAULT);
			// Parsing the results
			$row = oci_fetch_array($stmt, OCI_RETURN_NULLS);
			$detailsLob=$row['DETAILS'];
			if ($detailsLob == NULL) {
				return null;
			}
			else
			{
				return $detailsLob->load();
			}
			$this->connector->deconnection();
			}
	}
	
	public function listQueue(){
		$results = Array();
		//$results = "";
		$result = null;
			$EMS_SERVER = $GLOBALS['lbp_ems_serverUrl'];
			$EMS_USER_NAME = $GLOBALS['lbp_ems_userName'];
			$EMS_PASSWORD = $GLOBALS['lbp_ems_password'];
			$JAVA_HOME = $GLOBALS['lbp_java_home'];
			$JAR_LISTER = $GLOBALS['lbp_ems_queue_listing_jar_path'];
			//echo ($JAVA_HOME."java -jar ".$JAR_LISTER." ".$EMS_SERVER." ".$EMS_USER_NAME." ".$EMS_PASSWORD);
			$output = shell_exec($JAVA_HOME."java -jar ".$JAR_LISTER." ".$EMS_SERVER." ".$EMS_USER_NAME." ".$EMS_PASSWORD);
			$results = explode(PHP_EOL, $output);
			return $results;
	}
	public function resendMessageContent($messageData,$inputQueue){
			$result = null;
			$EMS_SERVER = $GLOBALS['lbp_ems_serverUrl'];
			$EMS_USER_NAME = $GLOBALS['lbp_ems_userName'];
			$EMS_PASSWORD = $GLOBALS['lbp_ems_password'];
			$JAVA_HOME = $GLOBALS['lbp_java_home'];
			$JAR_SENDER = $GLOBALS['lbp_ems_sender_jar_path'];
			$file = "/serveur_apps/EAIAdmin/temp/message.txt";
			file_put_contents($file, utf8_encode($messageData));
			$locale = 'fr_FR.UTF-8';
			setlocale(LC_ALL, $locale);
			putenv('LC_ALL='.$locale);
			$output = shell_exec($JAVA_HOME."java -jar ".$JAR_SENDER." ".$EMS_SERVER." ".$EMS_USER_NAME." ".$EMS_PASSWORD." ".$inputQueue." "."/serveur_apps/EAIAdmin/temp/message.txt");
			return $output;
	}
}
?>

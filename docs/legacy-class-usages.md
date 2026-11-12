# Legacy Class Usages
## These are the current usages of the legacy classes with the intent of being refactored out
## Lists every use of DssPluginCore, DebugCorePlugin, DssUtil, TimeOperation, WPPException, DSS*View and Eclipse types.
## Consider this a work queue

1) DssPluginCore
 - AddTimeWindowDialog (5)
   - createContents()
   - validateTW()
   - saveTWFile()
 - DecimalDialog (4)
   - createContents()
 - DateCombo (5)
   - setDateCombo()
   - procTWFile()
 - SchematicBase (87)
   - createPartControl()
   - makeActions()
   - retrieveUndebug()
   - calculateLongTermAverage()
   - calculateLongTermAverageSelectedMonths()
   - retrieveLongTermAverage()
   - retrieveLongTermAverageSelectedMonths()
   - retrieveDebug()
   - setValues()
   - unitsConversion()
   - collectAllSchematicVariables()
   - loadAllSchematicVariableData()
   - readFromSV()
2) DebugCorePlugin
- AddTimeWindowDialog (1)
  - saveTWFile()
- DecimalDialog (2)
  -  createContents()
- DateCombo (3)
  - procTWFile()
  - createControl()
- SchematicBase (17)
  - createPartControl()
  - retrieveUndebug()
  - retrieveLongTermAverage()
  - retrieveLongTermAverageSelectedMonths()
  - retrieveDebug()
  - loadAllSchematicVariableData()
3) DssUtil
- SchematicBase (1)
  - generatePathnameMap()
4) TimeOperation
- DateCombo (1)
  - setDateCombo() 
- SchematicBase (4)
  - calculateLongTermAverage()
  - calculateLongTermAverageSelectedMonths()
5) WPPException
- SchematicBase (3)
  - makeActions()
  - retrieveDebug()
- SchematicEditor (6)
  - makeEditorActions()
  - onCopy()
  - onPaste()
6) DSS*View
 * DSSCatalogView
 - SchematicBase (1)
   - createPartControl()
 * DSSFileView
 * DSSMonthlyView
 - SchematicBase (1)
   - createPartControl()
 * DSSOpsView
 * DSSPlotView
 - SchematicBase (1)
   - createPartControl()
 * DSSTableView
 - SchematicBase (1)
   - createPartControl() 
7) Eclipse types